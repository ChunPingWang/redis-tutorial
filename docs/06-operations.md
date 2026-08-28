# 06 · 維運與安全：上線後每天會用到的事

> Redis 上線之後，會出事的地方通常不是指令用錯，而是「沒設密碼被掃到」「記憶體滿了沒人知道」「備份從來沒還原過」「升級順序錯了資料歸零」。
> 這一章依照這些事故的發生順序來寫：先擋住外人（安全），再看得見問題（監控），再確保出事能救回來（備份還原），最後是日常操作與升級。
> 每個重點都先講「不做會怎樣」，再講「怎麼做」，最後才是指令。
> 腳本：`scripts/health-check.sh`、`scripts/backup.sh`；容器 / VM 的差異在 [02](02-deploy-container.md) / [03](03-deploy-vm.md)。

## 目錄

1. [安全：五道防線](#1-安全五道防線)
2. [ACL：最小權限帳號](#2-acl最小權限帳號)
3. [TLS](#3-tls)
4. [監控與告警](#4-監控與告警)
5. [備份與還原](#5-備份與還原)
6. [日常操作](#6-日常操作)
7. [升級](#7-升級)
8. [錯誤訊息對照表](#8-錯誤訊息對照表)
9. [上線檢查清單](#9-上線檢查清單)

---

## 1. 安全：五道防線

**這一節要解決的問題**：Redis 的預設設計是「信任內網」——沒有密碼、任何連上的人都能執行任何指令，包括改設定檔路徑和寫檔。這在 2009 年的內網很合理，在今天的雲端環境是災難。

**為什麼一定要在意**：沒有密碼、綁在 0.0.0.0 的 Redis，**幾分鐘內**就會被掃描器找到。攻擊手法很經典：`CONFIG SET dir /root/.ssh` + `CONFIG SET dbfilename authorized_keys` + 寫入一個含 SSH 公鑰的 key + `SAVE`，攻擊者就拿到這台機器的 root SSH；或者更直接——`FLUSHALL` 之後留一個勒索訊息。這不是理論，是每天都在發生的事。

**怎麼做**：安全不是一個開關，而是由外而內的多道防線——外層擋掉 99% 的掃描，內層確保即使有人進來也做不了太多事。少任何一層，其他層就得承受它的壓力。

| 防線 | 沒有它會怎樣 | 做法 | 對應設定 |
|---|---|---|---|
| 1 網路 | 全網際網路都能敲你的 6379 | 只有應用伺服器所在的網段能連 | 防火牆 / 安全群組；`bind 10.0.1.15`（內網 IP，不是 0.0.0.0） |
| 2 protected-mode | 忘了設密碼的那一刻就裸奔 | 沒設密碼時 Redis 自動拒絕非 loopback 連線——這是 Redis 3.2 之後加的「安全網」 | `protected-mode yes`（預設，不要關） |
| 3 認證 | 同網段任何一台被入侵的機器都能進來 | 密碼。注意 Redis 每秒可以驗證上萬次密碼，短密碼幾小時就能暴力破解，所以要 32 字元以上 | `requirepass` → 進一步用 ACL |
| 4 授權 | 應用程式被注入時，攻擊者拿到的是「全部指令」 | 每個應用一個帳號，只給它需要的指令與 key 前綴，出事時損害限縮在那個前綴 | ACL（[下一節](#2-acl最小權限帳號)） |
| 5 危險指令 | 一個 `FLUSHALL` 或 `CONFIG SET` 就能毀掉整台 | 應用帳號拿不到這些指令；就算有人拿到應用的連線也刪不了資料 | ACL `-@dangerous`；或 `rename-command` |

**另外三件常被忽略的事**，每一件都對應一個真實事故：
- **不要用 root 跑**：Redis 被攻破時，攻擊者拿到的是 Redis 行程的權限。用 root 跑等於送出整台機器；VM 腳本用 `redis` 帳號 + systemd 沙箱（`ProtectSystem`、`ProtectHome`），就算被攻破也只能碰資料目錄。
- **設定檔權限 640**：`redis.conf` 裡有明文密碼，644 的話同機任何帳號都能讀。
- **密碼不進程式碼與日誌**：`git log` 和 log 收集系統是密碼外洩最常見的來源；連線字串放 secret manager 或環境變數。

---

## 2. ACL：最小權限帳號

**這一節要解決的問題**：`requirepass` 只有一組密碼，所有應用共用——訂單服務、報表服務、監控都用同一把鑰匙，而且這把鑰匙能做任何事。任何一個服務外洩密碼，全部資料都暴露；任何一個服務的 bug（例如誤呼叫 `FLUSHDB`）都會影響所有人。

**怎麼做**：Redis 6+ 的 ACL 讓每個使用端有自己的帳號，帳號能做什麼由三個維度限制：**能用哪些指令**（用類別批次給，例如 `+@read`）、**能碰哪些 key**（用前綴 pattern，例如 `~order:*`）、**能訂閱哪些頻道**。原本的 `requirepass` 其實就是 `default` 使用者的密碼，ACL 是它的超集。

設計帳號時問三個問題：這個服務讀還是寫？碰哪些前綴？有沒有任何理由要用 `KEYS`、`FLUSHALL`、`CONFIG`？（答案幾乎永遠是沒有。）

```bash
# 訂單服務：只能碰 order:* 與 cache:*、只有讀寫類指令、明確拒絕危險類別
#   ~order:* ~cache:*  → 就算程式寫錯 key 前綴也會被拒，等於多一層防呆
#   +@read +@write     → 一般讀寫
#   +@hash +@list …    → 各資料結構的指令類別（@write 不涵蓋全部型別專屬指令）
#   -@dangerous        → FLUSHALL / KEYS / CONFIG / DEBUG / SHUTDOWN 等全部拿掉
ACL SETUSER app-order on >Str0ng-P@ss ~order:* ~cache:* +@read +@write +@hash +@list +@set +@sortedset +@stream -@dangerous

# 報表 / BI：只讀，但可以看全部前綴（它不會寫壞任何東西）
ACL SETUSER app-readonly on >ReadOnly-P@ss ~* +@read -@dangerous

# 監控（exporter）：只需要 INFO 與少數診斷指令；連 GET 都不給，因為它不需要讀資料
#   config|get 的寫法是「只給 CONFIG 的 GET 子指令」，不給 CONFIG SET
ACL SETUSER monitoring on >Mon-P@ss ~* +info +ping +client|list +slowlog +latency +memory +config|get

ACL LIST                              # 檢視結果
ACL WHOAMI                            # 現在是誰
ACL CAT dangerous                     # 看 dangerous 類別包含哪些指令，理解自己拒絕了什麼
```

**為什麼最後才關 `default`**：所有應用都換成新帳號之前，`default` 是它們唯一的入口；先關掉等於全站斷線。順序是：建帳號 → 應用改連線設定並驗證 → `ACL LOG` 確認沒有東西還在用 default → 關。

```bash
ACL SETUSER default off               # 確定所有應用都改用新帳號後才做
ACL SAVE                              # 寫到 aclfile，否則重啟就消失（redis.conf 要設 aclfile /etc/redis/users.acl）
ACL LOG                               # 被拒絕的存取：誰、用什麼帳號、想做什麼——排查權限不足與偵測可疑行為都靠它
```

連線方式：

```bash
redis-cli --user app-order --pass Str0ng-P@ss
AUTH app-order Str0ng-P@ss            # 應用程式的做法：先 AUTH 再下指令
```

規則語法速查：`on/off`（啟用）、`>密碼`（加一組密碼，可多組方便輪替）、`~pattern`（key）、`+指令 / -指令 / +@類別 / -@類別`、`&pattern`（Pub/Sub 頻道）、`allkeys / allcommands`（危險，只給管理員）。Module 13 的 `AclService` 有可執行的測試。

---

## 3. TLS

**這一節要解決的問題**：Redis 協定是明文，密碼與資料在網路上裸傳。在同一台機器或同一個私有子網內這不是問題；一旦流量經過你不完全控制的網路——跨機房、跨雲、經過共用交換器、或合規要求（PCI、個資法）——任何能看到封包的人都能拿到密碼與資料。

**什麼時候該開、代價是什麼**：TLS 讓每個指令多約 10–20% CPU（握手更貴，所以更要用連線池），Pipeline 的效益不變。判斷標準很簡單：流量離開你信任的網段就開；純內網、延遲敏感、且網段已隔離的可以不開。不確定就開。

**怎麼做**：需要用 `BUILD_TLS=yes` 編譯的 binary——VM 腳本偵測到 OpenSSL 開發套件就會開；apt 套件與官方容器映像都已支援。憑證用內部 CA 簽（讓 client 能驗證伺服器身分）；自簽憑證只適合練習。

```bash
# 練習用自簽憑證（正式環境用內部 CA 簽發，client 才能用 --cacert 驗證身分）
openssl req -x509 -newkey rsa:4096 -nodes -keyout redis.key -out redis.crt -days 365 -subj "/CN=redis"
```

```conf
port 0                      # 關掉明文 port：否則有人用明文連，TLS 等於白開
tls-port 6379
tls-cert-file /etc/redis/tls/redis.crt
tls-key-file  /etc/redis/tls/redis.key
tls-ca-cert-file /etc/redis/tls/ca.crt
tls-auth-clients no         # yes = 要求 client 也出示憑證（mTLS）：連密碼外洩都連不進來，代價是憑證要發給每個應用
tls-replication yes         # 主從之間的複寫流量也是完整資料，一樣要加密
tls-cluster yes             # Cluster 的 bus 也是
```

```bash
redis-cli --tls --cacert ca.crt -h redis.internal ping
```

---

## 4. 監控與告警

**這一節要解決的問題**：Redis 出問題的方式很安靜——記憶體滿了它只是開始淘汰或回 `OOM`，bgsave 失敗它只是在日誌寫一行，Replica 斷線 Master 照常服務。沒有監控的話，你會在使用者抱怨「快取好像沒用」「資料不見了」時才知道，而那時候已經沒有東西可以救。

**怎麼做**：分三層，由粗到細，因為它們回答的是不同問題：

1. **能不能連**（活著嗎）：`redis-cli ping`、exporter 的 `redis_up`。掛了要在 1 分鐘內知道。
2. **健不健康**（快出事了嗎）：`scripts/health-check.sh` 把記憶體、持久化狀態、拒絕連線、slowlog、複寫一次看完；exit code 非 0 就是有問題，可以直接接監控系統或 cron。
3. **趨勢**（會不會出事）：Prometheus + Grafana（`docker compose up -d` 內建儀表板）。記憶體「用了 80%」本身不是問題，「每天漲 5%」才是——只有趨勢圖看得出來。門檻見 [04 §8](04-performance-tuning.md#8-監控指標與告警門檻)。

**`INFO` 各區段該看什麼、看到什麼代表什麼**——這是所有監控工具的資料來源，看懂它就看懂儀表板：

```bash
redis-cli info server        # redis_version（升級後確認）、uptime_in_seconds（突然歸零 = 非預期重啟）、role
redis-cli info clients       # connected_clients 接近 maxclients = 連線洩漏；blocked_clients 多 = 有人在 BLPOP/WAIT 等太久
redis-cli info memory        # used_memory 逼近 maxmemory = 快淘汰 / 拒寫；mem_fragmentation_ratio >1.5 碎片、<1 用到 swap
redis-cli info persistence   # rdb_last_bgsave_status:err = 備份等於停了；rdb_changes_since_last_save 很大 = 一旦當機丟很多；loading:1 = 還在載入
redis-cli info stats         # rejected_connections >0 = 連線數上限；evicted_keys 增加 = 記憶體不夠；keyspace_hits/misses = 命中率；latest_fork_usec = fork 卡多久
redis-cli info replication   # master_link_status:down = Replica 斷線，HA 形同虛設；offset 差距持續變大 = Replica 追不上
redis-cli info keyspace      # 每個 db 的 keys / expires：keys 只漲不跌 = 有東西沒 TTL
redis-cli info errorstats    # 每種錯誤的次數（6.2+）：errorstat_OOM 出現 = 記憶體滿；errorstat_NOAUTH = 有人用錯密碼在敲
```

**最少要有的告警**（每一條對應一種「沒告警就會變成事故」的情況）：

| 告警 | 沒告警的後果 |
|---|---|
| Redis 掛了 / `redis_up = 0` | 應用全部打到資料庫，資料庫跟著倒 |
| 記憶體 > 90% | 幾小時後開始淘汰（快取命中率崩）或拒寫（`OOM`） |
| `rdb_last_bgsave_status != ok` | 從這一刻起沒有任何新備份，當機就回到上一次成功的快照 |
| Replica 斷線 | 下次 Master 掛掉時沒有東西可以接手 |
| `rejected_connections` 增加 | 部分請求已經失敗，只是還沒人回報 |
| slowlog 增加 | 有慢指令在擋所有人，延遲會逐步惡化 |

---

## 5. 備份與還原

**這一節要解決的問題**：持久化（RDB / AOF）只保護「Redis 行程掛掉」這一種情況；磁碟壞了、機器被刪、`FLUSHALL` 誤操作、勒索攻擊——持久化檔案跟資料一起消失。備份是把資料複製到**另一個故障域**（另一台機器、物件儲存），這才是真正的保險。

### RDB 備份（`scripts/backup.sh`）

**為什麼用 RDB 而不是 AOF 當備份**：RDB 是單一檔案、有壓縮、載入快，而且 Redis 寫 RDB 的方式是「寫到暫存檔 → 完成後 rename」，所以任何時刻複製 `dump.rdb` 拿到的都是完整檔案，不會抓到寫一半的狀態。AOF 是一個目錄、還在被持續追加，備份時要處理一致性問題。

**怎麼做**：觸發 `BGSAVE`（背景 fork，不擋主執行緒）→ 等它完成（`rdb_bgsave_in_progress` 回到 0，`LASTSAVE` 只有秒級精度不夠用）→ 複製檔案 → 用 `redis-check-rdb` 驗證檔案沒壞 → 保留最近幾份。`backup.sh` 就是這些步驟。

```bash
./scripts/backup.sh -a 密碼 -d /backup/redis        # BGSAVE → 等完成 → 複製 dump.rdb → redis-check-rdb 驗證 → 保留 7 份
# cron：每天 03:00（挑流量低谷，因為 fork 會有短暫延遲尖峰）
0 3 * * * /opt/redis-tutorial/scripts/backup.sh -a 密碼 -d /backup/redis >> /var/log/redis-backup.log 2>&1
```

遠端 Redis 或容器裡的 Redis 拿不到檔案系統時，用 `redis-cli --rdb backup.rdb`：它假裝成一個 Replica，透過複寫協定把 RDB 拉下來。

**備份檔一定要離開這台機器**：本機磁碟壞了什麼都沒了；送到另一台機器或物件儲存（S3 等），才算真的備份。

### AOF

**為什麼 AOF 也要備**：AOF 記錄到最後一秒的寫入，RDB 只到上次快照。要「盡量少丟」的還原點就得靠 AOF。

**怎麼做**：Redis 7 的 AOF 是目錄（`appendonlydir/`：base RDB + 增量檔 + manifest），三者要**整個目錄一起備**，缺一個就載入失敗。備份前先 `BGREWRITEAOF`，讓增量檔被壓成新的 base，備份檔會小很多。

損壞修復：Redis 當機時 AOF 可能寫到一半，啟動會拒絕載入。`redis-check-aof --fix appendonlydir/appendonly.aof.1.incr.aof` 會把損壞的尾巴截掉——代價是最後幾筆寫入消失，但整個檔案能用了。

### 還原

**為什麼要停 Redis 再放檔案**：Redis 只在啟動時讀持久化檔案，執行中換檔案不會有任何效果，而且下次 bgsave 還會把你放的檔案覆蓋掉。

**最常見的還原失敗——「還原了 RDB 但資料還是舊的」**：如果開了 AOF，Redis 啟動時**優先載入 AOF 並忽略 RDB**（因為 AOF 通常比較新）。你放好的 `dump.rdb` 根本沒被讀。所以還原 RDB 時要先把 `appendonlydir` 移開，載入後再重建 AOF。

```bash
sudo systemctl stop redis
sudo cp /backup/redis/dump-20260828.rdb /var/lib/redis/dump.rdb
sudo chown redis:redis /var/lib/redis/dump.rdb        # 權限不對 Redis 會讀不到而以空資料啟動——而且不會報錯得很明顯
sudo mv /var/lib/redis/appendonlydir /var/lib/redis/appendonlydir.old   # 讓 Redis 讀 RDB 而不是舊的 AOF
sudo systemctl start redis
redis-cli -a 密碼 dbsize             # 確認筆數與備份時一致：這一步不做，你不知道還原的是不是空的
redis-cli -a 密碼 BGREWRITEAOF        # 從目前的資料重建 AOF，之後的寫入才有 AOF 保護
```

容器：`docker run --rm -v redis-tutorial_redis-data:/data -v /backup:/backup alpine cp /backup/dump.rdb /data/dump.rdb`（同樣要先停掉 Redis 容器）。

### 演練

**為什麼「備份沒還原過等於沒備份」**：備份流程會壞得很安靜——cron 沒跑、磁碟滿了寫出 0 bytes 的檔案、權限改了複製不到、備份的是另一個實例。這些只有在真的拿備份檔還原時才會發現，而你最不希望第一次發現是在事故當下。

**怎麼做**：每季在測試機還原一次 → `DBSIZE` 對筆數 → 抽查幾個 key 的值 → 記錄整個流程花了多久。那個時間就是你真實的 RTO（能多快恢復），拿去跟業務承諾的比。

---

## 6. 日常操作

**這一節要解決的問題**：Redis 沒有「管理介面」，日常維運全靠指令。用錯指令的後果比一般資料庫嚴重（單執行緒：一個 `KEYS *` 擋住所有人），所以每組指令都要知道「什麼情境用、有什麼替代」。

**設定**——線上調參數，但要記得寫回檔案：

```bash
CONFIG GET maxmemory*                 # 先看目前值（含 maxmemory-policy）
CONFIG SET maxmemory 2gb              # 立即生效、不用重啟；記憶體快滿時的緊急手段
CONFIG REWRITE                        # 寫回 redis.conf——不做的話重啟就回到舊值，半夜重啟後才發現的事故很多
```

**連線**——找出誰在搞鬼、或在維護前擋住寫入：

```bash
CLIENT LIST                           # 誰連著、idle 多久、最後一個指令是什麼：找連線洩漏（同一 IP 幾百條 idle 連線）
CLIENT KILL ID 42 / CLIENT KILL ADDR 10.0.1.5:51234    # 踢掉異常 client（例如卡住的 MONITOR）
CLIENT PAUSE 5000 WRITE               # 暫停寫入 5 秒：手動切換 Master 前用，確保 Replica 追上、不丟最後幾筆
```

**資料**——查看與清理，避開會擋住全站的指令：

```bash
DBSIZE                                # key 總數，O(1)
SCAN 0 MATCH 'session:*' COUNT 1000   # 分批找 key；KEYS 會掃全部、百萬 key 時卡住數秒
OBJECT ENCODING key / MEMORY USAGE key / TTL key / TYPE key   # 診斷單一 key：編碼對不對、占多少、會不會過期
UNLINK bigkey                         # 背景釋放；DEL 大 key 會同步釋放百萬個元素，卡住主執行緒
```

**持久化**——手動觸發或確認狀態：

```bash
BGSAVE / LASTSAVE / BGREWRITEAOF      # 備份前、升級前手動存一次；LASTSAVE 看上次成功時間
INFO persistence                      # 確認 status 都是 ok 再進行任何危險操作
```

**複寫**——手動切換與確保寫入已同步：

```bash
INFO replication                      # 先看：誰是 Master、Replica 的 offset 有沒有追上
REPLICAOF NO ONE                      # 把這台升為 Master（Sentinel 環境不要手動做，交給 Sentinel）
REPLICAOF 10.0.1.11 6379              # 把這台指向新 Master；注意它會先清空自己的資料再全量同步
WAIT 1 1000                           # 等至少 1 個 Replica 確認收到寫入（最多等 1 秒）：重要寫入後用，降低切換時遺失的機率
```

**診斷**——延遲變高時的順序：

```bash
SLOWLOG GET 10                        # 先看有沒有慢指令
LATENCY DOCTOR                        # 再讓 Redis 自己分析（需要 latency-monitor-threshold）
MEMORY DOCTOR                         # 記憶體異常時
MONITOR                               # 最後手段：看即時指令流；每個指令都會複製一份給你，拖慢 Redis，Ctrl-C 要快
DEBUG SLEEP 0                         # DEBUG 系列會直接影響服務（SLEEP 讓 Redis 停住、RELOAD 重載資料），正式環境不要用
```

**關機**——確保資料寫完：

```bash
SHUTDOWN SAVE                         # 先存 RDB 再關；NOSAVE 直接關（只在確定不要資料時，例如純快取重建）
```

**Cluster 日常**（在任一節點執行）——擴容與維護：

```bash
redis-cli --cluster check 172.29.0.11:7001           # 任何操作前先確認 slot 全覆蓋、沒有 open slot
redis-cli --cluster info 172.29.0.11:7001            # 每個 Master 的 key 數與 slot 數，看分佈均不均
# 擴容：加節點本身不會有 slot，要再 rebalance / reshard 把 slot 搬過去，否則新節點是空的
redis-cli --cluster add-node 新節點:7007 172.29.0.11:7001                    # 加 Master
redis-cli --cluster add-node 新節點:7008 172.29.0.11:7001 --cluster-slave    # 加 Replica
redis-cli --cluster rebalance 172.29.0.11:7001 --cluster-use-empty-masters   # 自動平均分配 slot 給空的 Master
redis-cli --cluster reshard 172.29.0.11:7001                                 # 互動式搬指定數量的 slot
redis-cli -p 7001 CLUSTER FAILOVER    # 在 Replica 上執行：讓它平順地變 Master（升級或維護 Master 前用，資料不丟）
```

---

## 7. 升級

**這一節要解決的問題**：升級本身不難，難的是「不停機」和「升壞了能退回去」。兩個前提決定了整個流程：Redis 保證 RDB / AOF **向後相容**（新版讀得懂舊版檔案），但**反過來不保證**——用新版跑過之後寫出的檔案，舊版可能讀不了。所以升級前一定要先備份，那是唯一的回退路徑。

**單機**：會停機，時間 = 載入資料的時間。

```
備份 → 停 Redis → 換 binary / 套件 / image tag → 啟 → INFO server 確認版本 → DBSIZE 確認資料筆數沒變
```

載入 RDB 每 GB 約 10–20 秒；資料大又不能停太久，就該先做主從再升。

**主從 / Sentinel（零停機）**——順序是「先 Replica、後 Master」，理由有兩個：
1. Replica 升壞了只影響讀取，Master 還在服務；反過來先升 Master 壞了就是全站事故。
2. 新版 Replica 可以跟舊版 Master 同步（向後相容），舊版 Replica 跟新版 Master 不保證。

```
1. 逐台升 Replica（一次一台，升完看 master_link_status:up 再下一台）
2. CLIENT PAUSE 5000 WRITE → 在 Sentinel 上 SENTINEL FAILOVER mymaster   ← 讓一台已升級的 Replica 變 Master，寫入只暫停幾秒
3. 升舊 Master（現在是 Replica）
```

**Cluster**：每一對主從照上面做——先升 Replica，在它上面 `CLUSTER FAILOVER` 平順接手，再升原 Master。一對一對來，任何時候都有完整的 slot 覆蓋。

**大版本（例如 7 → 8）額外注意**：預設值會變（`aof-use-rdb-preamble`、`save` 的寫法）、有指令被移除、模組授權改變。先在測試環境用正式資料的備份跑一輪，再讀 release notes 的 "Breaking changes"。

---

## 8. 錯誤訊息對照表

**怎麼用這張表**：Redis 的錯誤訊息前綴（`NOAUTH`、`OOM`、`MOVED`…）就是分類；看到錯誤先對前綴，再看「為什麼會發生」判斷是設定問題、容量問題還是用法問題，最後才是處理。

| 錯誤 | 意思 | 為什麼會發生 | 處理 |
|---|---|---|---|
| `NOAUTH Authentication required` | 要密碼 | 設了 `requirepass` / ACL，client 沒先 AUTH | `AUTH` / `-a` / `--user --pass` |
| `WRONGPASS invalid username-password pair` | 帳密錯 | 密碼輪替後 client 沒更新；或 ACL 帳號名打錯 | 檢查 ACL；`ACL LOG` 看是誰 |
| `NOPERM this user has no permissions to run the 'x' command` | ACL 沒給這個指令 | 最小權限帳號本來就該擋；也可能是程式用了沒預期的指令 | 確認真的需要再 `ACL SETUSER … +指令` |
| `DENIED Redis is running in protected mode` | 沒密碼從外部連 | protected-mode 的安全網生效：沒設密碼又不是 loopback | 設密碼（正解）或 `protected-mode no`（只在隔離內網） |
| `OOM command not allowed when used memory > 'maxmemory'` | 記憶體滿且策略是 `noeviction` | 資料只增不減（沒 TTL）或容量估錯 | 加記憶體 / 改策略 / 清資料；長期看 `evicted_keys` 趨勢 |
| `MISCONF Redis is configured to save RDB snapshots, but it's currently unable to persist to disk` | bgsave 失敗，Redis 拒寫保護 | 磁碟滿、目錄權限、fork 失敗（overcommit）；Redis 寧可拒寫也不讓你以為有備份 | 看日誌修根因；緊急 `CONFIG SET stop-writes-on-bgsave-error no` 先恢復寫入 |
| `READONLY You can't write against a read only replica` | 寫到 Replica | 故障轉移後 client 還連著舊 Master（現在是 Replica） | 連 Master；Sentinel / Cluster 感知的 client 會自動處理 |
| `MOVED 5712 172.29.0.12:7002` | Cluster：key 在別的節點 | 每個 key 依 slot 歸屬固定節點，普通 client 不知道 | 用 cluster 模式的 client / `redis-cli -c` |
| `ASK 5712 …` | Cluster：slot 搬移中 | reshard 期間 key 可能在來源或目標 | 跟隨並先送 `ASKING`（cluster client 自動處理） |
| `CROSSSLOT Keys in request don't hash to the same slot` | Cluster 多 key 指令跨 slot | `MGET`、交易、Lua 的 key 落在不同節點，無法原子執行 | hash tag `{…}` 讓相關 key 同 slot |
| `CLUSTERDOWN The cluster is down` | slot 沒被覆蓋 | 某個 Master 掛了且 Replica 還沒升級（或沒有 Replica） | `--cluster check`；`cluster-require-full-coverage no` 可讓其他 slot 繼續服務 |
| `BUSY Redis is busy running a script` | Lua 跑太久 | 腳本裡有迴圈或處理大集合，超過 `busy-reply-threshold`（5 秒） | `SCRIPT KILL`（腳本還沒寫入時）/ `SHUTDOWN NOSAVE`；改寫腳本 |
| `LOADING Redis is loading the dataset in memory` | 啟動中載入 RDB / AOF | 資料大，載入要幾十秒到幾分鐘 | 等；常發生就拆成多個實例 |
| `NOREPLICAS Not enough good replicas to write` | `min-replicas-to-write` 沒滿足 | 你要求至少 N 個 Replica 在線才准寫，現在不夠——這是刻意的保護 | 檢查 Replica 狀態；別直接調低門檻 |
| `ERR max number of clients reached` | `maxclients` | 連線洩漏（沒用連線池）或 ulimit 太低讓 maxclients 被縮小 | 找洩漏；加大 + `LimitNOFILE` |
| `WRONGTYPE Operation against a key holding the wrong kind of value` | 型別用錯 | 同一個 key 名被不同程式當不同型別用 | `TYPE key`；key 命名要帶型別語意 |
| `EXECABORT Transaction discarded because of previous errors` | MULTI 裡有語法錯 | 排隊時就被發現的錯（參數數量、指令名），整個交易不執行 | 修指令 |
| `Can't save in background: fork: Cannot allocate memory` | overcommit | OS 認為沒有足夠記憶體給 fork 的子行程（其實 copy-on-write 用不到那麼多） | `vm.overcommit_memory=1` |
| Sentinel：`Can't resolve instance hostname` | 主機名稱解析 | `sentinel monitor` 用了名稱但沒開 `resolve-hostnames`，或該主機還沒起來 | 用 IP 或 `resolve-hostnames yes` |
| Sentinel：`+tilt` | 事件迴圈被卡 | DNS 查詢阻塞、系統時鐘跳動，Sentinel 不信任自己的計時而暫停判斷 | 用 IP；檢查 NTP |

---

## 9. 上線檢查清單

**怎麼用**：每一項後面都寫了「沒做的後果」——不是為了嚇人，而是讓你在被要求「先上線再說」時，能具體說出跳過這一項的風險是什麼，由業務決定要不要承擔。

**安全**
- [ ] `bind` 內網 IP；防火牆只開給應用 —— 否則掃描器幾分鐘內找到你
- [ ] 密碼 ≥ 32 字元；應用用 ACL 帳號、`default` 關閉 —— 否則一個服務外洩密碼 = 全部資料外洩
- [ ] 危險指令不給應用帳號 —— 否則一個 bug 或注入就能 `FLUSHALL`
- [ ] 非 root 執行；設定檔 640 —— 否則 Redis 被攻破 = 整台機器被攻破
- [ ] 跨網路傳輸有 TLS —— 否則密碼與資料在線路上裸傳

**資料**
- [ ] `maxmemory` 已設，≤ 實體記憶體 70%；策略符合用途 —— 否則被 OOM killer 殺掉，連淘汰的機會都沒有
- [ ] 持久化策略與 RPO 一致（everysec = 最多丟 1 秒）—— 否則出事時發現丟的比業務能接受的多
- [ ] 備份每天跑、送到異地、**還原演練過** —— 否則備份可能是空的而你不知道
- [ ] 所有快取 key 有 TTL —— 否則記憶體只漲不跌，幾週後開始淘汰

**可用性**
- [ ] 單點？至少主從；不能人工介入就 Sentinel（3 個、不同機器）—— 否則 Master 掛掉 = 停機到有人上線處理
- [ ] `min-replicas-to-write` 視資料重要性 —— 否則 Master 與 Replica 斷線期間寫入的資料在切換時消失
- [ ] 應用端：連線池、逾時、重試、Sentinel / Cluster 感知的 client —— 否則故障轉移成功了但應用還連著舊 Master

**效能**
- [ ] [04 檢查清單](04-performance-tuning.md#9-檢查清單) 全部打勾 —— 否則一個 `KEYS *` 就能讓全站延遲飆高
- [ ] OS：THP never、overcommit 1、日誌無 WARNING、`LimitNOFILE` —— 否則 fork 時延遲尖峰、bgsave 失敗、連線被拒

**監控**
- [ ] `health-check.sh` 或 exporter 接告警 —— 否則問題由使用者回報
- [ ] 告警：掛了、記憶體、bgsave 失敗、複寫斷線、拒絕連線、slowlog —— 每一條都對應第 4 節的一種事故
- [ ] 儀表板有人看 —— 否則趨勢型問題（記憶體每天漲 5%）沒人發現

**驗證**
- [ ] `scripts/verify-vm.sh`（VM）或 `scripts/verify-all.sh`（容器）通過 —— 否則你只是「覺得」設定生效了
- [ ] `smoke-test.sh` 通過 —— 否則資料型別或模組可能沒正確載入
- [ ] 在正式機型跑過 `benchmark.sh`，知道天花板 —— 否則流量成長時不知道離極限多遠
