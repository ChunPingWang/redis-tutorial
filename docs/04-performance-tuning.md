# 04 · 效能最佳化：先量測，再調整

> **這一章要解決的問題**：Redis 明明號稱每秒十萬次操作，為什麼我的應用還是慢？
> 答案幾乎都不在 Redis 本身，而在「怎麼用它」——每個指令都要跨一次網路、單執行緒會被一個慢指令擋住、一個大 key 能拖垮整台。
> 所以本章的順序就是排查的順序：**先量測知道慢在哪 → 網路往返 → 慢指令 → 大 key / 熱 key → 記憶體 → 持久化 → 多核與 OS**。
> 每一節都先講「為什麼會慢」，再講「怎麼看出來」，最後才是「改什麼」。
> 所有數字來自 `scripts/benchmark.sh` 在本專案 VM 環境的實測（Redis 8.10.1、16 vCPU、WSL2）。

## 目錄

1. [先量測：benchmark 與延遲工具](#1-先量測benchmark-與延遲工具)
2. [網路往返：Pipeline、批次指令、連線池](#2-網路往返pipeline批次指令連線池)
3. [慢指令：O(N) 與阻塞](#3-慢指令on-與阻塞)
4. [大 key 與熱 key](#4-大-key-與熱-key)
5. [記憶體最佳化](#5-記憶體最佳化)
6. [持久化對延遲的影響](#6-持久化對延遲的影響)
7. [多核、多實例、OS](#7-多核多實例os)
8. [監控指標與告警門檻](#8-監控指標與告警門檻)
9. [檢查清單](#9-檢查清單)

---

## 1. 先量測：benchmark 與延遲工具

**為什麼要先量測**：沒有基準數字，你不知道「慢」是慢在 Redis、網路、還是應用端；也不知道調整後有沒有變好。
很多人一開始就去改 `io-threads` 或加機器，結果問題其實是一個 `KEYS *`。
所以第一步永遠是：**先知道這台 Redis 的天花板在哪、現在的延遲長什麼樣**。

### redis-benchmark：知道天花板

**怎麼做**：`redis-benchmark` 是 Redis 自帶的壓測工具，會模擬多個連線送指令並統計吞吐量與延遲。
`scripts/benchmark.sh` 把最有教學意義的四組測試包起來：基準、pipeline、value 大小、單連線延遲分佈。

```bash
./scripts/benchmark.sh -a <密碼>        # 四組測試：基準 / pipeline / value 大小 / 延遲分佈
```

實測（單機、50 連線、10 萬請求）：

| 測試 | SET | GET | 這個數字告訴你什麼 |
|---|---|---|---|
| 基準（value 3 bytes） | **140k ops/s** | **151k ops/s** | p50 0.19 ms。CPU 沒滿，時間都花在等網路一來一回——瓶頸是**往返次數** |
| Pipeline 16 | **952k ops/s** | **1.43M ops/s** | 同樣的指令、同一台機器，**快 6.8 / 9.5 倍**，只因為一次送 16 個。證明上一列的推論 |
| value 1 KB | 121k | 129k | 比 3 bytes 慢一點點，還在可接受範圍 |
| value 100 KB | **5.7k** | 28k | 掉 25 倍：每秒 570 MB 已吃滿頻寬。**大 value 的成本是線性的** |
| 單連線往返（-c 1） | | p50 **0.079 ms**、p99 0.23 ms | 這是 loopback 的物理極限；跨機器再加 0.1–0.5 ms，跨機房加 1–2 ms |

從這張表得到三個結論，它們就是後面幾節的依據：
1. **單一指令的成本 ≈ 一次網路往返**——所以減少往返次數（第 2 節）是最有效的優化
2. **大 value 的成本是線性的**——100 KB 的 value 等於 3 萬個小 value（第 4 節）
3. 單機 15 萬 ops/s 是「一個連線一個指令」的天花板；要更高只能 pipeline 或分片（第 7 節）

### 延遲工具：找出現在慢在哪

**為什麼需要不只一個工具**：延遲有兩種——「一直都慢」（網路、慢指令）和「偶爾尖峰」（fork、AOF fsync、大量 key 同時過期）。
一直都慢用 `--latency` 就看得到；偶爾尖峰要靠 `--latency-history` 看週期、`latency doctor` 看是哪種事件。

```bash
redis-cli --latency                 # 持續量測往返延遲（min / max / avg）：看「平常」多快
redis-cli --latency-history         # 每 15 秒一筆：看有沒有週期性尖峰（= fork / AOF rewrite）
redis-cli --intrinsic-latency 5     # 不經 Redis，量「機器本身」的延遲；VM 上 > 100 µs 就是虛擬化層的問題，Redis 怎麼調都沒用

redis-cli latency doctor            # 需要 latency-monitor-threshold > 0；它會直接告訴你原因
redis-cli latency latest            # 各事件（command / fork / aof-write / expire-cycle）最近一次尖峰

redis-cli slowlog get 10            # 超過 slowlog-log-slower-than（10 ms）的指令：誰、什麼時候、多久
redis-cli slowlog reset

redis-cli info commandstats         # 每種指令的呼叫次數與平均耗時（usec_per_call）
redis-cli info stats | grep -E "latest_fork_usec|instantaneous_ops"
```

**為什麼 `INFO commandstats` 最好用**：slowlog 只記錄超過門檻的單筆指令，但「每次 0.7 ms、一秒被叫一萬次」的指令永遠不會進 slowlog，卻能吃掉整個 CPU。
commandstats 按指令種類累計，一眼就看出哪一種在吃時間：

```
cmdstat_hgetall:calls=12034,usec=8420000,usec_per_call=699.7   ← 平均 0.7 ms，遠高於其他指令 → 有大 Hash
cmdstat_get:calls=9812345,usec=9812345,usec_per_call=1.0       ← 正常的指令是 1 µs 等級
```

---

## 2. 網路往返：Pipeline、批次指令、連線池

**這一節要解決的問題**：第 1 節證明了「一個指令 ≈ 一次往返」。Redis 執行一個 `GET` 只要 1 µs，但往返要 100–500 µs——
也就是說 **99% 的時間 Redis 在等你**。這一節的所有手段都只做一件事：**用更少的往返送更多的工作**。

### Pipeline（Module 05）

**為什麼有效**：一般模式是「送一個、等回覆、再送下一個」，每個指令都付一次往返。Pipeline 是「一口氣送 N 個、一口氣收 N 個回覆」，N 個指令只付一次往返。
實測 16 個一批就快 6.8–9.5 倍，而且 Redis 端 CPU 幾乎沒增加——因為它本來就閒著。

**怎麼做**：Redis 不需要任何設定，是 client 端的行為。`redis-cli --pipe` 從 stdin 讀指令：

```bash
for i in $(seq 1 100000); do echo "SET key:$i $i"; done | redis-cli --pipe
# All data transferred. Waiting for the last reply... Last reply received from server. errors: 0, replies: 100000
```

Java（Lettuce / Spring Data Redis）：`redisTemplate.executePipelined(...)`。

**為什麼不是越大越好**：Redis 要為每個回覆配置輸出緩衝區，一批 10 萬個會讓記憶體瞬間暴漲、其他 client 也要等這一整批做完。每批 100–1000 個是常見的甜蜜點。

### 批次指令

**為什麼**：很多指令天生就有「多 key / 多欄位」版本，一個指令做完 N 件事，和 pipeline 一樣只付一次往返，而且還是**原子的**（中間不會插入別人的指令）。
迴圈呼叫單 key 版本，等於自願付 N 次往返。

| 不要（N 次往返） | 改用（1 次往返） | 額外好處 |
|---|---|---|
| 迴圈 `GET k1`、`GET k2`… | `MGET k1 k2 …` | 一次拿到一致的快照 |
| 迴圈 `HSET h f v` | `HSET h f1 v1 f2 v2 …` | 原子 |
| 迴圈 `SADD s m` | `SADD s m1 m2 …` | 原子 |
| 先 `GET` 再判斷再 `SET` | `SET … NX` / `INCR` / Lua | 兩次往返之間別人可能改了值（競態）；單一指令沒有這個問題 |

### Lua 腳本（Module 05）

**為什麼**：「讀 → 判斷 → 寫」這種邏輯用一般指令要兩三次往返，而且往返之間狀態可能被別人改掉。Lua 在伺服器端執行，一次往返、整段原子。

**代價與規則**：Lua 執行時整個 Redis 停住（單執行緒），所以：
- 只做幾個指令的組合，不做迴圈運算——你是在借用所有人的時間
- 超過 `busy-reply-threshold`（5 秒）其他 client 會收到 `BUSY`
- 用 `EVALSHA` 避免每次傳腳本本體（省頻寬）

### 連線池

**為什麼**：建立一條連線 = TCP 三次握手 + `AUTH` + `SELECT` + `CLIENT SETNAME`，至少三四次往返，比一個 `GET` 貴幾十倍。每個請求開新連線，等於每個 `GET` 都先付這筆稅。
**怎麼看出來**：`INFO stats` 的 `total_connections_received` 一直漲，`connected_clients` 卻不高，就是沒用連線池。
**怎麼做**：Lettuce 預設是單一長連線多工（thread-safe），不用 pool；Jedis 需要 pool。

### Client-side caching（Redis 6+，RESP3）

**為什麼**：最快的往返是「不往返」。讀多改少的熱資料，應用端自己快取一份，完全不打 Redis——但問題是「資料改了怎麼知道」。
**怎麼做**：`CLIENT TRACKING ON` 讓 Redis 記住你讀過哪些 key，被改動時主動通知你讓本地快取失效。這是熱 key 的終極解法（第 4 節）。

---

## 3. 慢指令：O(N) 與阻塞

**這一節要解決的問題**：Redis 是單執行緒（[01 §2](01-architecture.md#2-內部架構單執行緒事件迴圈)），**一個慢指令，所有人一起等**。
一個對百萬元素集合的 `SMEMBERS` 花 500 ms，這 500 ms 內其他一萬個 client 的 `GET` 全部排隊——你會看到「Redis 偶爾整體變慢」，但 `INFO` 的 CPU 很低，非常難查。
所以規則只有一條：**每個指令都要在微秒級完成**；凡是複雜度跟資料量成正比（O(N)）的指令，都要問「N 會多大」。

| 危險指令 | 為什麼危險 | 改用 |
|---|---|---|
| `KEYS pattern` | 掃**全部** key，百萬 key 時擋住所有人數秒；正式環境最常見的事故來源 | `SCAN 0 MATCH pattern COUNT 100` 分批，每批只擋幾十 µs |
| `SMEMBERS` / `HGETALL` / `LRANGE 0 -1` / `ZRANGE 0 -1` 對大集合 | 一次序列化並回傳全部元素，時間與記憶體都跟元素數成正比 | `SSCAN` / `HSCAN` / `ZSCAN` 分批，或 `LRANGE 0 99` 分頁 |
| `DEL bigkey` | 同步釋放百萬個元素的記憶體，主執行緒卡住 | `UNLINK bigkey`（背景執行緒釋放）；設 `lazyfree-lazy-user-del yes` 讓 `DEL` 自動變 `UNLINK` |
| `FLUSHALL` / `FLUSHDB` | 同步清空整個資料庫 | `FLUSHALL ASYNC` |
| `SORT` | O(N log N)，還要配置暫存 | 應用端排序，或改用 Sorted Set（寫入時就排好） |
| `SUNION` / `SINTER` / `ZUNIONSTORE` 對大集合 | O(N×M)，兩個十萬元素的集合就是百億次比較 | 預先計算結果存起來、或縮小集合 |
| `MONITOR` | 每個指令都要多複製一份給你，吞吐量直接掉一半 | 只在除錯時短暫用；正式環境看 slowlog |
| `SAVE`（不是 `BGSAVE`） | 在主執行緒同步寫 RDB，資料大時停幾秒 | `BGSAVE`（fork 子行程去寫） |
| `DEBUG SLEEP` / `DEBUG RELOAD` | 顧名思義會停住 / 重載 | 不要在正式環境用 `DEBUG` |
| 大量 key 同時過期 | 定期刪除迴圈發現過期比例高就會一直做下去，變成長時間阻塞 | TTL 加隨機抖動，讓過期時間分散 |

**`SCAN` 為什麼安全、怎麼正確用**：它每次只走一小段 hash table 就回傳，並給你一個游標讓你下次繼續。
代價是可能重複（不會漏），`COUNT` 只是提示不是保證：

```bash
redis-cli --scan --pattern 'session:*' | head        # redis-cli 內建的 SCAN 包裝
redis-cli --scan --pattern 'session:*' | xargs -n 100 redis-cli unlink   # 分批刪：每批 100 個，且用 UNLINK
```

---

## 4. 大 key 與熱 key

**這一節要解決的問題**：兩種「單一 key 拖垮整台」的情況。
**大 key**（一個 key 有幾十萬個元素或幾 MB）：讀取、刪除、過期、複寫、AOF rewrite 全都要處理整個 key，每一步都是第 3 節的慢指令；Cluster 也無法把一個 key 拆到多個節點。
**熱 key**（一個 key 承受大部分流量）：所有請求打在同一個節點的同一個執行緒上，加機器、加分片都沒用，因為它永遠只在一個地方。
兩者共同的難處是：**平常看不出來**，要主動找。

### 找出來

**為什麼要工具**：大 key 不會在 `INFO` 出現，`DBSIZE` 只告訴你 key 數。Redis 提供的掃描工具會用 `SCAN` 走一遍（安全）並統計。

```bash
redis-cli --bigkeys                 # 用 SCAN 掃，各型別最大的 key（依元素數）
redis-cli --memkeys                 # 依實際記憶體（MEMORY USAGE）：元素少但每個很大的 key 這裡才看得到
redis-cli --hotkeys                 # 依存取頻率；需要 maxmemory-policy 是 *lfu（LFU 才會記錄頻率）
redis-cli memory usage user:1001    # 單一 key 占多少 bytes
redis-cli object freq user:1001     # LFU 計數（*lfu 策略下）
redis-cli debug object user:1001    # 編碼、序列化長度（serializedlength）
```

判斷標準（經驗值）：String > 10 KB、集合類 > 5000 個元素或 > 1 MB，就算大 key——超過這個量級，第 3 節的每一種操作都會進 slowlog。

### 大 key 的處理

**原則**：把「一個大 key 的 O(N)」變成「N 個小 key 的 O(1)」，讓每次操作只碰需要的那一份。

| 情況 | 為什麼這樣拆 | 做法 |
|---|---|---|
| 大 Hash / Set（百萬欄位） | 讀寫只碰一個 bucket；Cluster 也能分到不同節點 | 依 id 取模拆成 N 個：`user:1001:orders:{0..15}`，讀時算 bucket |
| 大 List 當佇列 | List 沒有長度上限、沒有 ACK，只會一直長 | 改 Stream（有 consumer group、可 `MAXLEN ~ 10000` 自動裁切） |
| 大 String（JSON） | 每次讀寫都傳整包，吃頻寬 | 應用端壓縮（gzip / snappy）；或改用 JSON 型別只讀寫需要的路徑 |
| 大 Sorted Set 排行榜 | 通常只需要前 N 名 | 只保留前 N：`ZREMRANGEBYRANK lb 0 -1001` |
| 刪除既有大 key | 一次 `DEL` 會卡住 | `UNLINK`；集合類先 `SSCAN` 分批 `SREM` 再刪 |

### 熱 key 的處理

**原則**：熱 key 的本質是「同一份資料被同一個執行緒服務」，解法只有兩個方向——**不要打 Redis**，或**把它複製成多份分散開**。

1. **應用端本地快取**（Caffeine + 短 TTL，或第 2 節的 client-side caching）：最有效，因為連往返都省了
2. **複製 N 份**：寫時 `key:0 … key:N-1` 都寫，讀時隨機挑一個——Cluster 會把它們 hash 到不同節點，流量就分散了
3. **讀寫分離**：讀走 Replica，把讀流量從 Master 分出去（但 Replica 有複寫延遲）

---

## 5. 記憶體最佳化

**這一節要解決的問題**：Redis 的資料全在記憶體，記憶體就是成本、也是容量上限。
而且 Redis 的記憶體開銷跟直覺不同：一個 value 只有 5 bytes 的 key，實際要 60–70 bytes（[01 §3](01-architecture.md#3-記憶體模型一個-key-到底占多少)）——
**開銷主要在 key 的數量與包裝，不在資料本身**。所以省記憶體的思路是「減少 key 數、用省記憶體的編碼、讓沒用的資料消失」。

### 看清楚記憶體去哪了

**為什麼要分開看**：`used_memory` 是 Redis 認為自己用了多少，`used_memory_rss` 是 OS 實際給了多少；兩者的比值（碎片率）告訴你是「資料多」還是「碎片多」還是「用到 swap」——三種情況的處理完全不同。

```bash
redis-cli info memory
# used_memory_human:1.2G           資料 + Redis 內部結構（Redis 自己算的）
# used_memory_rss_human:1.5G       OS 實際配置給行程的
# mem_fragmentation_ratio:1.25     rss / used；>1.5 碎片多，<1 表示部分記憶體被換到 swap（延遲會爆）
# used_memory_peak_human           歷史高點：規劃 maxmemory 時參考
# used_memory_overhead             非資料的開銷（連線緩衝、複寫 backlog、dict 本身）
# used_memory_dataset              真正的資料
redis-cli memory stats              # 更細：keys.bytes-per-key（平均每個 key 幾 bytes）、overhead.total
redis-cli memory doctor             # 它會直接給建議
```

### 減少開銷的手段（依效果排序）

1. **合併小 key 成 Hash**：為什麼——每個 key 都有 60 bytes 的包裝，一萬個 `user:N:name` String 就是 600 KB 的純開銷；Hash 在小規模時用 listpack 編碼，欄位是緊密排列的，幾乎沒有包裝。怎麼做——1 萬個 String → 100 個各含 100 欄位的 Hash，可省 5–10 倍。前提是欄位數 ≤ `hash-max-listpack-entries`（128）且值 ≤ 64 bytes，超過就會轉成 hashtable、優勢消失（[01 §3](01-architecture.md#3-記憶體模型一個-key-到底占多少)）。
2. **縮短 key**：為什麼——key 名稱本身也占記憶體，而且每個 key 都要存一份。`user:1001:profile` 比 `application:user-service:user:1001:profile` 少 30 bytes，× 千萬個 key = 300 MB。
3. **給 TTL**：為什麼——沒有 TTL 的快取只會漲，直到 `maxmemory` 開始淘汰（那時你已經在靠運氣決定誰被丟）。
4. **選對 `maxmemory-policy`**：為什麼——淘汰策略決定記憶體滿時「丟誰」，丟錯會讓命中率崩掉。有明顯熱點的快取用 `allkeys-lfu`（依頻率）比 LRU（依最近使用）命中率高。
5. **壓縮大 value**：為什麼——第 1 節實測 100 KB value 慢 25 倍，壓縮同時省記憶體與頻寬。應用端 gzip；或把 JSON 的欄位名縮短。
6. **調編碼門檻**（小心）：`hash-max-listpack-entries 256` 讓更多 Hash 留在省記憶體的編碼。代價——listpack 是 O(N) 掃描，欄位太多會讓每次 `HGET` 變慢，等於拿第 3 節的問題換記憶體。
7. **碎片整理**：為什麼——大量刪除後，記憶體被還給配置器但沒還給 OS，`rss` 居高不下。`activedefrag yes`（Redis 4+，jemalloc 才有效）在背景搬移資料。只有 `mem_fragmentation_ratio > 1.5` 且 `used_memory` 夠大時才需要——小資料集的碎片率沒有意義。
8. **32-bit 編譯**：資料 < 4 GB 時指標少一半——現在幾乎沒人用。

### 淘汰的觀察

**為什麼看這幾個數字**：`evicted_keys` 一直漲代表記憶體不夠、Redis 正在替你丟資料；命中率掉代表被丟的是還會用到的資料——這兩個一起看，就知道是「容量不夠」還是「TTL 設錯」。

```bash
redis-cli info stats | grep -E "evicted_keys|expired_keys|keyspace_hits|keyspace_misses"
```

命中率 = hits / (hits + misses)，快取低於 80% 就該檢查 TTL 與容量。

---

## 6. 持久化對延遲的影響

**這一節要解決的問題**：「Redis 每隔幾分鐘就卡一下」——這種**週期性尖峰**幾乎都來自持久化。
RDB 快照與 AOF 重寫都要 fork 子行程，fork 期間主執行緒停住；AOF 的 fsync 在磁碟慢時也會擋住主執行緒。
你要做的不是關掉持久化，而是**知道每種設定付出多少延遲、換回多少安全**，然後選一個符合需求的組合。

### fork

**為什麼 fork 會卡**：fork 要複製父行程的**頁表**（不是資料——資料靠 copy-on-write 共享），頁表大小跟行程用的記憶體成正比，而複製頁表期間主執行緒不能動。

| 資料集 | fork 時間（無 THP、實體機） |
|---|---|
| 1 GB | ~10–20 ms |
| 10 GB | ~100–300 ms |
| 30 GB+ | 秒級 → 這麼大請拆成多個實例 |

**怎麼看**：`INFO stats` 的 `latest_fork_usec`。
**為什麼 VM 更慢、THP 一定要關**：VM 的巢狀分頁讓頁表操作更貴；而 THP 開著時，fork 後父行程每次寫入要複製 2 MB 的 huge page 而不是 4 KB——寫入越多、延遲越高、記憶體暴漲。這就是 [03 §7](03-deploy-vm.md#7-os-調校) 堅持 THP `never` 的原因。

### AOF fsync

**為什麼有三種選擇**：fsync 是「確保資料真的寫到磁碟」，越常做越安全、越常做越慢。這是用延遲換資料安全的旋鈕：

| `appendfsync` | 延遲影響 | 資料遺失 | 什麼時候選 |
|---|---|---|---|
| `no` | 無（交給 OS，通常 30 秒 flush 一次） | 最多 30 秒 | 純快取、有 Replica 當備援 |
| `everysec` | 背景執行緒每秒 fsync；磁碟慢時主執行緒最多被擋 2 秒 | 最多 1 秒 | **預設與大多數場景的選擇** |
| `always` | 每個寫入都等 fsync，吞吐量掉 10–50 倍 | 0（理論上） | 真的一筆都不能丟——但通常寧可用主從 + `WAIT` |

`no-appendfsync-on-rewrite yes`：為什麼——rewrite 期間子行程大量寫磁碟，此時主行程再 fsync 會互相搶 I/O，主執行緒被擋更久；設 yes 讓 rewrite 期間不 fsync，代價是那段時間可能丟 30 秒。
**怎麼看是不是磁碟的問題**：`latency latest` 出現 `aof-fsync-always` / `aof-write-pending-fsync` 事件，就是磁碟跟不上。

### 把持久化移到 Replica

**為什麼**：Master 完全不做 fork，就沒有 fork 尖峰；由 Replica 負責寫 RDB / AOF。
**怎麼做**：Master `save ""` + `appendonly no`，Replica 開持久化。
**代價**：Master 重啟後是空的，要從 Replica 同步回來。**絕不能**把 Master 設成自動重啟且自動變回 Master——它會帶著空資料集把 Replica 也同步成空的。

---

## 7. 多核、多實例、OS

**這一節要解決的問題**：第 1 節的 15 萬 ops/s 天花板來自「指令執行只用一個核心」。一台 16 核的機器跑一個 Redis，15 個核心在閒置。
這一節講怎麼用上其餘的核心，以及 OS 層有哪些設定會讓單核的效率打折。

### I/O threads（Redis 6+）

**為什麼有用、為什麼有限**：單執行緒的時間其實一大半花在「讀網路封包、解析協定、寫回覆」而不是執行指令。I/O threads 把這部分分給其他核心，指令執行仍是單執行緒——所以最多提升約 2 倍，不會線性成長。

```conf
io-threads 4              # 建議 = 核心數 - 1，最多 8；4 核以下不用開（切換成本大於收益）
io-threads-do-reads yes
```

實測在高連線數、小 value 的場景約提升 1.5–2 倍；pipeline 已經開很大時效果有限（因為往返已經很少了）。

### 多實例 / Cluster

**為什麼**：要真正用滿多核，只能跑多個 Redis 行程。
**怎麼做**：一台 16 核機器跑 4–8 個實例（不同 port、各自 `maxmemory`），用 Cluster 組起來（[01 §6](01-architecture.md#6-四種拓撲單機主從sentinelcluster)）。
**為什麼每個實例不要太大**：每個實例 `maxmemory` ≤ 10–15 GB，第 6 節的 fork 才不會太慢。

### OS（[03 §7](03-deploy-vm.md#7-os-調校) 有完整版與理由）

- THP `never`、`vm.overcommit_memory=1`、`vm.swappiness=1`：分別對應 fork 延遲、fork 失敗、資料進 swap
- 把 Redis 綁在固定 CPU（`taskset`），並把網卡中斷綁到別的核心：避免 Redis 的核心一直被中斷打斷
- 關掉 NUMA 的自動 balancing 或用 `numactl --interleave=all`：跨 NUMA 節點存取記憶體慢 30–50%
- 網卡：關 `irqbalance` 對 Redis 核心的干擾；開 `net.ipv4.tcp_tw_reuse` 避免短連線耗盡 port
- 容器：`--network host` 省掉 bridge 的一層 NAT

### Client 端

**為什麼 client 端也算效能**：第 1 節說了單一指令 ≈ 一次往返，往返時間是由網路距離決定的——Redis 端再快也救不了 2 ms 的跨區延遲。

- 應用與 Redis 放同一個可用區；跨區每次往返 +1–2 ms，等於把 Redis 變慢 10 倍
- 逾時設短（100–500 ms）並重試：否則一個慢的 Redis 會讓應用的執行緒全部卡住、拖垮整個服務
- 監控 client 端的 p99：Redis 端的指標看不到網路與連線池的等待

---

## 8. 監控指標與告警門檻

**為什麼是這幾個指標**：它們各對應前面某一節的一種失敗模式。看到哪個指標異常，就回到那一節找原因——這張表是全章的索引。

| 指標（`INFO` / exporter） | 正常 | 告警 | 為什麼要看它 |
|---|---|---|---|
| `used_memory / maxmemory` | < 80% | > 90% | 接近就開始淘汰（快取）或拒寫（`noeviction`）→ 第 5 節 |
| `mem_fragmentation_ratio` | 1.0–1.5 | > 1.5 或 < 1 | > 1.5 碎片浪費記憶體；< 1 是用到 swap，延遲會從 µs 變 ms → 第 5 節 |
| `evicted_keys` 增速 | 0 | 持續 > 0（非快取用途） | Redis 正在替你丟資料 → 第 5 節 |
| `keyspace_hits / (hits+misses)` | > 90%（快取） | < 80% | 快取沒發揮作用：TTL 太短或容量不夠 → 第 5 節 |
| `instantaneous_ops_per_sec` | 依基準 | 接近單核天花板 | 快到極限了，要 pipeline / 分片 → 第 2、7 節 |
| `connected_clients` | 依連線池 | 接近 `maxclients` | 連線洩漏；到了上限新連線全部被拒 → 第 2 節 |
| `rejected_connections` | 0 | > 0 | 已經有連線被拒：`maxclients` 或 ulimit 不夠 |
| `latest_fork_usec` | < 100 ms | > 500 ms | fork 尖峰：資料集太大或 THP 沒關 → 第 6 節 |
| `rdb_last_bgsave_status` / `aof_last_bgrewrite_status` | ok | err | 持久化失敗，下一次重啟就是資料遺失；且預設會拒寫（`MISCONF`） |
| `master_link_status`（Replica） | up | down | 複寫斷線：故障轉移時沒有可用的 Replica |
| `master_repl_offset - slave_repl_offset` | 小 | 持續增大 | Replica 追不上：讀到舊資料、故障轉移丟更多 |
| slowlog 筆數增速 | 0 | > 0 | 有慢指令在擋住所有人 → 第 3 節 |
| `rdb_changes_since_last_save` | 依 save 設定 | 很大且很久沒存 | 快照一直沒成功，遺失窗口越來越大 |

Grafana 儀表板（`docker compose up -d`）已把這些都畫好。

---

## 9. 檢查清單

**為什麼要有這張表**：前面八節的每一條都對應一種真實事故；上線前逐條確認，比事故後翻文件便宜得多。每一項後面標了「沒做會怎樣」。

- [ ] 沒有 `KEYS`、`SMEMBERS`/`HGETALL` 大集合、`FLUSH*` 出現在應用程式碼（grep 一次）——否則某天流量一大就整台卡住（§3）
- [ ] 批次操作用 `MGET` / pipeline；每批 ≤ 1000——否則付 N 倍往返（§2）
- [ ] 連線池；逾時 ≤ 500 ms——否則每個請求付握手稅、Redis 慢時拖垮應用（§2、§7）
- [ ] 所有快取 key 有 TTL，且有隨機抖動——否則記憶體只漲不跌、或同時過期造成雪崩（§3、§5）
- [ ] `--bigkeys` 沒有 > 1 MB 或 > 5000 元素的 key——否則讀刪複寫都是慢指令（§4）
- [ ] `maxmemory` 已設、策略正確、機器記憶體 ≥ maxmemory × 1.5——否則 OOM killer 直接殺掉、或 fork 失敗（§5、§6）
- [ ] THP never、overcommit 1、swappiness 1、日誌無 WARNING——否則 fork 延遲暴增 / bgsave 失敗 / 資料進 swap（§6、§7）
- [ ] `latest_fork_usec` < 100 ms——否則每次快照都是一次可感知的停頓（§6）
- [ ] `slowlog-log-slower-than 10000` 並有人在看——否則慢指令出現時沒有證據（§1）
- [ ] Grafana / 告警接上第 8 節的門檻——否則第一個發現問題的是使用者
- [ ] 用 `redis-benchmark` 在正式機型上量過基準，知道天花板在哪——否則不知道「慢」是相對於什麼（§1）
