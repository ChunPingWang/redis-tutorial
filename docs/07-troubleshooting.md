# 07 · 問題排查：12 個情境模擬與排查順序

> 出事的時候最貴的不是修，是「不知道從哪裡看起」。這一章先給一套固定的排查順序，
> 再用 12 個真實會發生的情境練習——每個情境都能用 `scripts/scenario.sh inject N` **真的弄壞**、
> 照著排查順序找出根因、再 `reset N` 還原。全部 12 個情境都由 `scripts/verify-scenarios.sh` 自動驗證過。

## 目錄

1. [排查心法：為什麼要有固定順序](#1-排查心法為什麼要有固定順序)
2. [第一分鐘：五個指令看全貌](#2-第一分鐘五個指令看全貌)
3. [症狀 → 情境索引](#3-症狀--情境索引)
4. [情境 S1–S8：單機](#4-情境-s1s8單機)
5. [情境 S9、S12：Sentinel](#5-情境-s9s12sentinel)
6. [情境 S10：Cluster](#6-情境-s10cluster)
7. [情境 S11：ACL](#7-情境-s11acl)
8. [驗證](#8-驗證)

---

## 1. 排查心法：為什麼要有固定順序

**沒有順序會怎樣**：看到「Redis 很慢」就先重啟——重啟後症狀消失、日誌也沒了，下週再發生一次。或看到 OOM 就加記憶體，其實是沒設 TTL，加多少都會滿。排查的目標不是讓症狀消失，是**找到根因並確認它**；順序是為了不漏、不繞路。

**怎麼排**：由外而內、由便宜到昂貴。

```
1. 症狀確認    它到底回了什麼錯？（錯誤字串就是最好的線索，見 06 §8 對照表）
2. 連得上嗎    PING → 連不上是網路 / 連線數 / 行程死了，這三者的處理完全不同
3. Redis 自述  INFO 五個區段 + SLOWLOG + LATENCY：Redis 自己知道的都在這裡
4. 誰在做什麼  CLIENT LIST / commandstats：找出是哪個 client、哪個指令
5. 資料本身    --bigkeys / --hotkeys / SCAN 抽樣 TTL：問題常在資料的形狀
6. OS 與磁碟   df / dmesg / THP / swap：Redis 說「寫不進去」時才往這裡
7. 修復        先止血（讓服務恢復）再根治（改設定 / 改程式），兩者分開記錄
8. 預防        這個根因怎麼提早看到？加告警或加檢查清單
```

**三個原則**：
- **先讀錯誤訊息再猜**：Redis 的錯誤字串（`OOM`、`MISCONF`、`NOPERM`、`CLUSTERDOWN`…）幾乎都直接指出類別。
- **重啟之前先蒐證**：`INFO ALL`、`SLOWLOG GET 128`、`CLIENT LIST`、最近 200 行日誌存下來；重啟會清掉 slowlog 與統計。
- **一次只改一個東西**：改完看指標有沒有變，才知道是不是它。

---

## 2. 第一分鐘：五個指令看全貌

**為什麼是這五個**：它們涵蓋「活著嗎 / 記憶體 / 持久化 / 誰在連 / 有沒有慢指令」五個最常見的故障類別，全部只讀、不影響服務，一分鐘內跑完就能把問題縮到一個方向。

```bash
redis-cli -a 密碼 --no-auth-warning ping                         # 1. 活著嗎（回什麼錯就是方向）
redis-cli info memory | grep -E "used_memory_human|maxmemory_human|maxmemory_policy|mem_fragmentation_ratio"
redis-cli info persistence | grep -E "rdb_last_bgsave_status|aof_last_write_status|loading"
redis-cli info stats | grep -E "instantaneous_ops|rejected_connections|evicted_keys|expired_keys|keyspace_hits|keyspace_misses|latest_fork_usec"
redis-cli info clients | grep -E "connected_clients|blocked_clients|maxclients"
redis-cli slowlog get 10                                          # 5. 最近的慢指令
redis-cli latency latest                                          #    以及延遲事件（需 latency-monitor-threshold）
```

`scripts/health-check.sh` 把這些包成一支腳本並加上判斷；排查時先跑它。

看到什麼往哪走：

| 看到 | 方向 | 情境 |
|---|---|---|
| PING 回 `max number of clients` | 連線耗盡 | S3 |
| PING 連線被拒 / 逾時 | 行程死了或網路；`docker logs` / `journalctl -u redis` | S8 |
| `used_memory` ≈ `maxmemory` | 記憶體滿 | S1、S4 |
| `rdb_last_bgsave_status:err` | 持久化失敗 | S5 |
| `rejected_connections` 增加 | 連線數 | S3 |
| `evicted_keys` 增加 | 記憶體不夠，正在淘汰 | S4 |
| `keyspace_hits` 遠小於 `misses` | 快取沒發揮作用 | S4 |
| slowlog 有東西 | 慢指令 / 大 key | S2 |
| `latest_fork_usec` 很大 | fork 太慢（資料大 / THP） | [04 §6](04-performance-tuning.md#6-持久化對延遲的影響) |
| `loading:1` | 啟動中載入資料 | S8 |

---

## 3. 症狀 → 情境索引

| 使用者 / 監控說的話 | 情境 |
|---|---|
| 「寫入都失敗，讀取正常」 | S1（OOM）、S5（MISCONF） |
| 「偶爾卡一下，幾百毫秒」 | S2（大 key / 慢指令） |
| 「新的連線全部連不上，舊的還能用」 | S3 |
| 「記憶體一直漲，重啟才會掉」「快取好像沒用」 | S4 |
| 「資料全部不見了」 | S6（也可能是 S1/S4 淘汰、或 TTL） |
| 「Redis CPU 100%，但 QPS 沒有特別高」 | S7（熱 key）、S2 |
| 「重啟後起不來」 | S8 |
| 「Master 掛了，但沒有自動切換」 | S9 |
| 「Cluster 部分 key 讀不到，回 CLUSTERDOWN」 | S10 |
| 「應用一直收到 NOPERM / NOAUTH」 | S11 |
| 「Replica 資料舊的 / Sentinel 說 Replica 下線」 | S12 |

每個情境的格式固定：**情境**（你會聽到的描述）→ **先問自己**（縮小範圍的三個問題）→ **排查順序**（每步看什麼、看到什麼代表什麼）→ **根因** → **修復（止血 / 根治）** → **預防** → **自己模擬**。

---

## 4. 情境 S1–S8：單機

> 準備：`docker compose up -d redis`。以下 `redis-cli` 指令在容器內執行：`docker exec -it redis-tutorial redis-cli`。

### S1 寫入被拒：`OOM command not allowed when used memory > 'maxmemory'`

**情境**：應用日誌大量 `OOM command not allowed`，讀取全部正常，只有寫入失敗。

**先問自己**：這台 Redis 的用途是快取還是資料？`maxmemory-policy` 是什麼？記憶體是「本來就這麼多資料」還是「突然漲上來」？

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | 錯誤字串 `OOM command not allowed` | Redis 自己拒絕的，不是 OS 的 OOM killer——那種情況是行程直接消失 |
| 2 | `INFO memory`：`used_memory_human` vs `maxmemory_human`、`maxmemory_policy` | used ≥ max 且 policy 是 `noeviction` → 這就是原因：滿了又不准淘汰 |
| 3 | `INFO errorstats`：`errorstat_OOM:count=…` | 從什麼時候開始、影響多少次寫入 |
| 4 | `INFO keyspace`：`keys` vs `expires` | expires 遠小於 keys → 大部分 key 沒有 TTL，會一直漲（轉 S4） |
| 5 | `redis-cli --bigkeys` / `--memkeys` | 是幾個大 key 吃光，還是均勻地多 |

**根因**：`maxmemory` 3 MB、`noeviction`，資料 4 MB。真實世界的版本是：maxmemory 設得比資料量小、或快取沒 TTL 漲到上限、或 policy 該用 `allkeys-lru` 卻用預設的 `noeviction`。

**修復**：
- 止血：`CONFIG SET maxmemory 8gb`（機器有空間的話）或 `CONFIG SET maxmemory-policy allkeys-lru`（純快取可以）——寫入立刻恢復
- 根治：依用途決定 policy（[01 §4](01-architecture.md#4-過期與淘汰記憶體滿了怎麼辦)）；快取 key 全部加 TTL；`CONFIG REWRITE` 寫回設定檔

**預防**：告警 `used_memory / maxmemory > 80%`；`health-check.sh` 會警告 maxmemory 未設或使用率 > 90%。

**自己模擬**：`./scripts/scenario.sh inject 1` → `redis-cli SET a 1` 看到 OOM → 照上表排查 → `reset 1`。

---

### S2 延遲飆高：大 key 與 O(N) 指令

**情境**：p99 延遲從 1 ms 跳到 100 ms 以上，不是持續的，是一陣一陣；QPS 沒變。

**先問自己**：是所有指令都慢，還是某幾個？有沒有週期性（每分鐘 / 每小時）？最近有沒有新功能上線？

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | `SLOWLOG GET 10` | 直接列出超過 10 ms 的指令、key 名、client 位址。實測：`SMEMBERS scenario:bigset` 132 ms、`DEL` 80 ms |
| 2 | `INFO commandstats` 的 `usec_per_call` | 哪一種指令平均很慢；`slowlog_count`、`slowlog_time_ms_max` 欄位（Redis 7+）直接統計 |
| 3 | `redis-cli --bigkeys` | 找出 `scenario:bigset` 有 30 萬個成員——這就是慢的原因 |
| 4 | `redis-cli --latency-history` | 週期性尖峰 → 可能是 fork（`latest_fork_usec`）而不是指令 |
| 5 | `LATENCY DOCTOR` | 讓 Redis 自己判斷（需 `latency-monitor-threshold`） |

**根因**：一個 30 萬元素的 Set，被 `SMEMBERS`（O(N) 回傳全部）與 `DEL`（同步釋放）操作。單執行緒下，這 132 ms 內所有其他 client 都在等。

**修復**：
- 止血：把呼叫 `SMEMBERS` 的程式改成 `SSCAN` 分批；刪除用 `UNLINK`（背景釋放）
- 根治：拆 key（[04 §4](04-performance-tuning.md#4-大-key-與熱-key)）；開 `lazyfree-lazy-user-del yes` 讓 `DEL` 自動變 `UNLINK`

**預防**：`slowlog-log-slower-than 10000` 並監控 `SLOWLOG LEN`；上線前 `--bigkeys` 掃一次；code review 禁止 `KEYS`、`SMEMBERS`、`HGETALL` 對未知大小的集合。

**自己模擬**：`inject 2`（會建 100 萬元素的 Set 並執行 SMEMBERS / DEL，留下 30 萬的大 key）→ `SLOWLOG GET 5` → `reset 2`。

---

### S3 連不上：`ERR max number of clients reached`

**情境**：新的應用實例啟動就報錯連不上；已經連著的還能用；Redis 行程活著、CPU 很低。

**先問自己**：連線數是慢慢漲上來的還是突然的？應用有沒有用連線池？最近有沒有新服務接上來？

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | 錯誤字串 `max number of clients reached` | 不是網路問題，是 Redis 主動拒絕 |
| 2 | 你也連不進去 → 只能從**已有的連線**或**旁路**查：`docker exec` 進容器內用 unix socket、或先殺掉幾個占線的 | 排查工具本身也需要一條連線，這就是為什麼監控要**保留一條常駐連線** |
| 3 | 連得上之後 `INFO clients`：`connected_clients` vs `maxclients` | 兩者相等就是滿了 |
| 4 | `CLIENT LIST` 看 `idle=`、`cmd=`、`name=`、`addr=` | idle 很大的一堆連線 = 連線洩漏（應用開了沒關）；同一個 addr 幾百條 = 沒用連線池 |
| 5 | `INFO stats` 的 `rejected_connections` | 事後統計被拒絕了多少次 |

**根因**：`maxclients` 太小或連線洩漏。模擬用 `maxclients 3` + 3 條閒置訂閱連線；真實世界是應用每個請求開新連線不關、或 `maxclients` 沒配合 `ulimit -n`。

**修復**：
- 止血：`CLIENT KILL ADDR …` 或 `CLIENT KILL TYPE pubsub` 殺掉洩漏的；`CONFIG SET maxclients 10000`（需 `LimitNOFILE` 足夠，見 [03 §6](03-deploy-vm.md#6-systemd-服務)）
- 根治：應用改用連線池、設 `timeout 300` 讓 Redis 主動踢閒置連線

**預防**：告警 `connected_clients > maxclients × 0.8`；`rejected_connections > 0` 就告警。

**自己模擬**：`inject 3` → `redis-cli PING` 被拒 → `reset 3`（從外部刪掉占線的容器，Redis 本身已經連不進去）。

---

### S4 記憶體只漲不跌、命中率低：快取沒有 TTL

**情境**：記憶體曲線是一條斜向上的直線，重啟才會掉；快取命中率只有 25%，資料庫負載沒有降。

**先問自己**：這些 key 應該有 TTL 嗎？誰負責刪？命中率低是「沒存進去」還是「存了但一直 miss 不同的 key」？

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | `INFO keyspace`：`db0:keys=5000,expires=0` | **5000 個 key 沒有一個有 TTL**——它們永遠不會自己消失 |
| 2 | `INFO stats`：`keyspace_hits:1000 keyspace_misses:3000` | 命中率 25%；miss 多是因為查的 key 根本沒存過（穿透）或 TTL 太短 |
| 3 | `SCAN` 抽樣 + `TTL key` | `-1` = 永不過期；`-2` = 不存在 |
| 4 | `INFO memory` 的 `used_memory` 趨勢 + `evicted_keys` | 漲到 maxmemory 後開始淘汰 → 變 S1 或命中率更差 |
| 5 | 看程式：`SET` 有沒有帶 `EX`、`HSET` 後有沒有 `EXPIRE` | 常見錯誤：`HSET` 不會設 TTL |

**根因**：快取寫入時沒帶 TTL。

**修復**：
- 止血：對現有 key 補 TTL：`SCAN` + `EXPIRE`（批次、分散時間）；或切 `allkeys-lru` 讓它自己淘汰
- 根治：所有快取寫入帶 `EX` 並加隨機抖動（[05 §1](05-use-cases.md#1-快取cache-aside)）；穿透用空值快取或布隆過濾器

**預防**：定期抽樣 `TTL` 為 `-1` 的 key 比例；告警命中率 < 80%（[04 §8](04-performance-tuning.md#8-監控指標與告警門檻)）。

**自己模擬**：`inject 4` → `INFO keyspace`、`INFO stats` → `reset 4`。

---

### S5 拒寫：`MISCONF Redis is configured to save RDB snapshots, but it's currently unable to persist to disk`

**情境**：所有寫入失敗，錯誤訊息很長；讀取正常；沒有人改過設定。

**先問自己**：磁碟滿了嗎？資料目錄權限對嗎？最近有沒有動過目錄、掛載、容器 volume？

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | 錯誤字串 `MISCONF … unable to persist to disk` | Redis 的自我保護：bgsave 失敗且 `stop-writes-on-bgsave-error yes` → 拒寫，避免你以為資料有存 |
| 2 | `INFO persistence`：`rdb_last_bgsave_status:err`、`rdb_changes_since_last_save` | 確認是 RDB 失敗；changes 數字 = 已經沒存到的寫入量 |
| 3 | Redis 日誌：`Failed opening the temp RDB file … Permission denied` | **日誌直接寫出原因**：權限 / 磁碟滿（`No space left`）/ fork 失敗（`Cannot allocate memory`） |
| 4 | `df -h 資料目錄`、`ls -ld 資料目錄`、`id redis` | 對應日誌的原因 |
| 5 | `dmesg`、`sysctl vm.overcommit_memory` | fork 失敗的情況 |

**根因**：資料目錄 `/data` 被改成 `dr-xr-xr-x`（555），Redis 使用者寫不進去。真實世界：磁碟滿、備份腳本改了權限、容器 volume 掛成唯讀、overcommit 沒開讓 fork 失敗。

**修復**：
- 止血：修好磁碟 / 權限後 `BGSAVE` 一次，狀態回 `ok` 寫入就自動恢復；真的暫時修不好可以 `CONFIG SET stop-writes-on-bgsave-error no`（**你正在接受「資料沒有持久化」**）
- 根治：磁碟容量告警、目錄權限納入佈署腳本（`install-redis.sh` 的第 2 步就是在做這件事）

**預防**：告警 `rdb_last_bgsave_status != ok`、`aof_last_write_status != ok`；`health-check.sh` 會標紅。

**自己模擬**：`inject 5` → `SET a 1` 看到 MISCONF → 看 `docker logs redis-tutorial` → `reset 5`。

---

### S6 資料全部消失：`FLUSHALL`

**情境**：某個時間點之後所有快取 miss、Session 全部登出；`DBSIZE` 是 0；Redis 沒重啟過。

**先問自己**：是全部消失還是部分？（部分 → TTL 或淘汰）Redis 有重啟過嗎？（`uptime_in_seconds`）誰有權限下 `FLUSHALL`？

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | `DBSIZE`、`INFO keyspace` | 0 → 全部消失；部分減少 → 看下一步 |
| 2 | `INFO stats`：`evicted_keys`、`expired_keys` | 兩者都沒漲 → 不是淘汰也不是過期，是有人刪 |
| 3 | `INFO server` 的 `uptime_in_seconds` | 剛重啟且沒持久化 → 是重啟丟的（轉 [01 §5](01-architecture.md#5-持久化架構rdbaof混合)） |
| 4 | `INFO commandstats`：`cmdstat_flushall:calls=1` | **有人執行了 FLUSHALL**；`cmdstat_flushdb`、`cmdstat_del` 也看一下 |
| 5 | 誰？`ACL LOG`（若有 ACL）、應用日誌、`CLIENT LIST` 的 `name`/`addr`（若還連著） | 沒有 ACL 的話 Redis 不記錄誰做的——這就是要 ACL 的原因 |

**根因**：某個 client 執行了 `FLUSHALL`。常見來源：測試環境的程式碼連到正式環境、Spring 的 `@CacheEvict(allEntries=true)` 誤用、某人在 RedisInsight 按錯。

**修復**：
- 止血：從備份還原（[06 §5](06-operations.md#5-備份與還原)）；純快取則讓它從資料庫重新暖機（注意資料庫瞬間壓力）
- 根治：ACL 收掉 `FLUSHALL / FLUSHDB`（`-@dangerous`），或 `rename-command FLUSHALL ""`

**預防**：每個應用一個 ACL 帳號、`default` 關閉；告警 `DBSIZE` 驟降。

**自己模擬**：`inject 6` → `DBSIZE`、`INFO commandstats | grep flush` → `reset 6`。

---

### S7 CPU 單核打滿：熱 key

**情境**：Redis 所在的那一個核心 100%，其他核心閒著；QPS 沒有異常；延遲全面上升；Cluster 環境下只有一個節點這樣。

**先問自己**：流量是不是集中在少數 key？最近有沒有活動 / 熱門商品？有沒有大 key 被高頻讀（S2 + S7 的組合最常見）？

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | `INFO commandstats` | `cmdstat_get:calls=300000` 短時間內某指令暴增 |
| 2 | `CONFIG SET maxmemory-policy allkeys-lfu`（暫時）→ `redis-cli --hotkeys` | LFU 才有存取頻率；`--hotkeys` 直接列出最熱的 key |
| 3 | `OBJECT FREQ key` | 實測熱 key 的 LFU 計數 249（上限 255），冷 key 是 5 |
| 4 | `MONITOR` 幾秒（`redis-cli monitor | head -1000`） | 直接看指令流；**只能短暫用**，它會讓吞吐量減半 |
| 5 | `CLIENT LIST` 的 `cmd=` 與 `tot-cmds=` | 哪個 client 在打 |

**根因**：一個 key 承受絕大部分讀取。單執行緒下它就是那一顆核心的天花板；Cluster 把它分到某個節點，那個節點就是瓶頸。

**修復**：
- 止血：應用端加本地快取（幾秒 TTL 就能把 Redis 的壓力降 100 倍）
- 根治：複製 N 份隨機讀、client-side caching（[04 §4](04-performance-tuning.md#4-大-key-與熱-key)）

**預防**：正式環境用 `allkeys-lfu`（`--hotkeys` 才有資料）；監控單核 CPU 而不是整機平均。

**自己模擬**：`inject 7` → `redis-cli --hotkeys`、`OBJECT FREQ key:000000000000` → `reset 7`。

---

### S8 Redis 起不來：`Bad file format reading the append only file`

**情境**：機器重開後 Redis 一直重啟；`PING` 連線被拒；日誌最後一行是 `Bad file format reading the append only file … at offset 85`。

**先問自己**：重開之前有沒有不正常關機（斷電、`kill -9`、容器被 OOM kill）？磁碟有沒有滿過？有沒有備份？

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | `docker logs` / `journalctl -u redis` 最後 20 行 | `Bad file format` = AOF 內容損毀；`short read` = 尾端截斷（通常自動容忍）；`Cannot allocate memory` = 記憶體不夠載入 |
| 2 | 錯誤訊息裡的檔名與 offset | 告訴你哪個檔、壞在哪；Redis 同時提示了兩種修法 |
| 3 | **先備份整個 `appendonlydir/`** | `--fix` 是破壞性的，修壞了沒有回頭路 |
| 4 | `redis-check-aof 檔名`（不加 `--fix`） | 只檢查：`ok_up_to=…` 告訴你有效資料到哪裡 |
| 5 | 評估損失：`--fix` 會**截掉損壞點之後的所有資料**（實測：4215 bytes 縮成 23 bytes，100 筆寫入全丟） | 損失可接受 → `--fix`；不可接受 → 從備份還原（[06 §5](06-operations.md#5-備份與還原)），損失是「備份到現在」 |

**根因**：AOF 中段有非法內容。尾端截斷（斷電時最常見）會被 `aof-load-truncated yes` 自動處理並記一行 warning；中段損毀（磁碟壞軌、檔案被誤改、rewrite 途中出錯）才會拒絕啟動。

**修復**：
- 止血：`redis-check-aof --fix <incr 檔>` → 啟動 → `DBSIZE` 確認；或還原備份
- 根治：查為什麼會損毀（`dmesg` 看磁碟錯誤、確認關機流程是 SIGTERM 而不是 SIGKILL、`TimeoutStopSec` 夠長）

**預防**：每日備份並演練還原；systemd 的 graceful shutdown（[03 §6](03-deploy-vm.md#6-systemd-服務)）；監控 `aof_last_write_status`。

**自己模擬**：`inject 8`（會先 rewrite、寫 100 筆、再覆寫協定框架）→ `docker logs redis-tutorial --tail 5` → `reset 8`（用 `redis-check-aof --fix`，觀察它截掉多少）。

---

## 5. 情境 S9、S12：Sentinel

> 準備：`docker compose stop redis`（釋放 6379）→ `docker compose -f docker-compose-sentinel.yml up -d`。Sentinel 指令：`docker exec sentinel-1 redis-cli -p 26379 …`。

### S9 Master 掛了卻沒有自動切換

**情境**：Master 已經停了 30 秒，應用全部寫入失敗；Sentinel 日誌只有 `+sdown master`，沒有 `+odown`、沒有 `+switch-master`。

**先問自己**：有幾個 Sentinel 活著？quorum 設多少？Sentinel 之間連得到嗎？（[02 §3](02-deploy-container.md#3-sentinel-環境docker-compose-sentinelyml) 的 DNS/TILT 問題是另一個常見原因）

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | `SENTINEL CKQUORUM mymaster` | 實測：`NOQUORUM 3 usable Sentinels. Not enough available Sentinels to reach the specified quorum`——**一句話講完** |
| 2 | `SENTINEL MASTER mymaster`：`flags`、`quorum`、`num-other-sentinels` | `s_down,master`（只有主觀下線）、`quorum 5`、其他 Sentinel 只有 2 個 → 永遠湊不到 5 |
| 3 | Sentinel 日誌 | `+sdown` 有、`+odown` 沒有 = 個別 Sentinel 都知道掛了，但達不到共識 |
| 4 | 若 quorum 正常仍不切換：日誌找 `+tilt`、`failover-abort`、`-failover-abort-no-good-slave` | TILT = Sentinel 自己卡住（DNS / 時鐘）；no-good-slave = 沒有合格的 Replica（`replica-priority 0`、資料太舊、斷線） |
| 5 | `SENTINEL SENTINELS mymaster` | 互相看不到 → 網路 / `announce-ip` 問題 |

**根因**：quorum（5）大於 Sentinel 總數（3）。真實世界：Sentinel 掛了兩個沒人發現、quorum 設成「Sentinel 數量」而不是「過半」、或不同機器的 Sentinel 互相連不到。

**修復**：
- 止血：`SENTINEL SET mymaster quorum 2`（每個 Sentinel 都要下）→ 幾秒內 `+odown` → 切換；或手動 `SENTINEL FAILOVER mymaster`
- 根治：quorum = ⌊N/2⌋+1 並且 N ≥ 3；Sentinel 本身也要監控

**預防**：定期（或告警）跑 `SENTINEL CKQUORUM`；Sentinel 行程加入健康檢查。

**自己模擬**：`inject 9` → `SENTINEL CKQUORUM mymaster` → `reset 9`。

---

### S12 Replica 斷線、複寫延遲

**情境**：讀走 Replica 的應用拿到舊資料；Sentinel 日誌 `+sdown slave`；Master 的 `connected_slaves` 從 2 變 1。

**先問自己**：Replica 行程活著嗎？網路通嗎？Master 的輸出緩衝有沒有爆（`client-output-buffer-limit replica`）？

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | Master：`INFO replication` | `connected_slaves:1`、剩下的 `slave0:…state=online,offset=…,lag=1` → 少了一台 |
| 2 | Replica：`INFO replication` | `master_link_status:down`、`master_link_down_since_seconds`、`master_sync_in_progress` |
| 3 | Replica 日誌 | `Connection with master lost`、`MASTER aborted replication with an error`、`Timeout receiving bulk data` |
| 4 | Master 日誌 | `Disconnecting timedout replica`、`client scheduled to be closed ASAP for overcoming of output buffer limits` → 緩衝爆了，資料大時全量同步永遠完成不了 |
| 5 | 恢復後：Replica 日誌 `Partial resynchronization` vs `Full resync` | partial = `repl-backlog` 夠大、很快追上；full = 要重傳整份 RDB，Master 要 fork |

**根因**：Replica 停止。真實世界：Replica 機器重開、網路分割、輸出緩衝太小、`repl-timeout` 太短。

**修復**：
- 止血：把 Replica 拉起來；同步期間讀流量先切回 Master
- 根治：`repl-backlog-size` 加大（斷線期間的寫入量 × 2）讓它能 partial resync；`client-output-buffer-limit replica 512mb 128mb 60`

**預防**：告警 `connected_slaves` 減少、`master_link_status:down`、`master_repl_offset - slave offset` 持續增大。

**自己模擬**：`inject 12` → Master `INFO replication` → `reset 12`（觀察 Replica 日誌是 partial 還是 full resync）。

---

## 6. 情境 S10：Cluster

> 準備：`docker compose -f docker-compose-cluster.yml up -d`。指令在容器內：`docker exec redis-node-2 redis-cli -p 7002 …`。

### S10 `CLUSTERDOWN The cluster is down`

**情境**：一部分 key 的讀寫回 `CLUSTERDOWN`，另一部分正常；剛好有一台機器維護重開。

**先問自己**：哪些 slot 沒人服務？那組主從是不是在同一台機器 / 同一個可用區？`cluster-require-full-coverage` 是什麼？

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | `CLUSTER INFO` | 實測：`cluster_state:fail`、`cluster_slots_ok:10923`、`cluster_slots_fail:5461` → 三分之一的 slot 沒人服務 |
| 2 | `CLUSTER NODES` 找 `fail` | `172.29.0.11:7001 master,fail … 0-5460` 與 `172.29.0.15:7005 slave,fail` 是**同一組主從** → 沒有 Replica 可以升級 |
| 3 | `redis-cli --cluster check <任一節點>` | 列出 `[ERR] Not all 16384 slots are covered` 與哪些 slot |
| 4 | 那兩台為什麼一起掛：放在同一台實體機 / 同一可用區？ | 這是拓撲規劃問題，不是 Redis 問題 |
| 5 | `cluster-require-full-coverage` | `yes`（預設）：任一 slot 不可用整個叢集拒絕服務；`no`：其他 slot 繼續服務——本專案設 `no`，所以其他三分之二的 key 仍正常 |

**根因**：一個 Master 與它唯一的 Replica 同時下線。Cluster 只能容忍「每組主從至少活一個」。

**修復**：
- 止血：把任一台拉起來（Replica 起來會升為 Master）；都起不來則 `CLUSTER ADDSLOTS` 把 slot 指給別的節點（**該 slot 的資料就沒了**）
- 根治：`--cluster-replicas 2`（每個 Master 兩個 Replica）；確保主從不在同一故障域（`redis-cli --cluster create` 會依 IP 盡量分開，但容器 / VM 的實體位置它不知道）

**預防**：告警 `cluster_state != ok`、`cluster_slots_fail > 0`；佈署時檢查每組主從的實體位置。

**自己模擬**：`inject 10`（停掉 node-1 與它的 Replica）→ `CLUSTER INFO`、`CLUSTER NODES` → `reset 10`。

---

## 7. 情境 S11：ACL

### S11 應用一直收到 `NOPERM`

**情境**：新版應用上線後某些功能失敗，錯誤 `NOPERM User scenario-app has no permissions to run the 'set' command`；舊功能正常。

**先問自己**：這個帳號的 ACL 規則是什麼？新功能用了哪些新指令 / 新 key 前綴？

**排查順序**：

| 步 | 看什麼 | 看到什麼代表什麼 |
|---|---|---|
| 1 | 錯誤字串 | `NOPERM … command` = 指令不在允許清單；`NOPERM … key` = key 不符合 `~pattern`；`NOAUTH` / `WRONGPASS` 是另一類 |
| 2 | `ACL LOG` | 實測：`reason: command`、`object: set`、`username: scenario-app`、`count: 2`、`client-info` 含來源位址——**誰、用哪個帳號、做什麼被拒、幾次**全部在這 |
| 3 | `ACL GETUSER scenario-app` | `commands: -@all +@read`、`keys: ~app:*` → 只能讀 `app:*` |
| 4 | 對照新功能需要的指令與 key | 缺什麼補什麼，**不要**直接給 `allcommands` |

**根因**：ACL 是最小權限，新功能需要寫入但帳號只有 `+@read`。

**修復**：`ACL SETUSER scenario-app +@write`（或更精確的 `+set +hset`）→ `ACL SAVE`。

**預防**：ACL 規則放在版本控制；新功能上線前在測試環境用相同 ACL 跑一遍；監控 `ACL LOG` 的數量。

**自己模擬**：`inject 11` → `ACL LOG`、`ACL GETUSER scenario-app` → `reset 11`。

---

## 8. 驗證

```bash
./scripts/verify-scenarios.sh            # 12 個情境：注入 → 斷言排查指令看得到症狀 → 還原 → 斷言恢復；約 5 分鐘
./scripts/verify-scenarios.sh single     # 只跑單機的 9 個
```

每個情境驗證三件事：**症狀真的出現**（錯誤字串、INFO 欄位、日誌）、**排查指令真的看得到根因**（SLOWLOG、--bigkeys、CKQUORUM、ACL LOG…）、**還原後恢復正常**。實測結果見 [README 的驗證章節](../README.md#驗證方式與實測紀錄)。
