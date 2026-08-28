# 04 · 效能最佳化：先量測，再調整

> Redis 很快，慢的通常是**用法**。這一章的順序就是排查的順序：
> 量測 → 找出慢在哪 → 網路往返 → 慢指令 → 大 key / 熱 key → 記憶體 → 持久化 → 多核與 OS。
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

### redis-benchmark

```bash
./scripts/benchmark.sh -a <密碼>        # 四組測試：基準 / pipeline / value 大小 / 延遲分佈
```

實測（單機、50 連線、10 萬請求）：

| 測試 | SET | GET | 說明 |
|---|---|---|---|
| 基準（value 3 bytes） | **140k ops/s** | **151k ops/s** | p50 0.19 ms；瓶頸在網路往返 |
| Pipeline 16 | **952k ops/s** | **1.43M ops/s** | **快 6.8 / 9.5 倍**，CPU 幾乎沒增加 |
| value 1 KB | 121k | 129k | 還好 |
| value 100 KB | **5.7k** | 28k | 掉 25 倍：每秒 570 MB 已吃滿頻寬 |
| 單連線往返（-c 1） | | p50 **0.079 ms**、p99 0.23 ms | 這是 loopback；跨機器加 0.1–0.5 ms |

三個結論：
1. **單一指令的成本 ≈ 一次網路往返**——所以減少往返次數是最有效的優化
2. **大 value 的成本是線性的**——100 KB 的 value 等於 3 萬個小 value
3. 單機 15 萬 ops/s 是「一個連線一個指令」的天花板；要更高就 pipeline 或分片

### 延遲工具

```bash
redis-cli --latency                 # 持續量測往返延遲（min / max / avg）
redis-cli --latency-history         # 每 15 秒一筆，看有沒有週期性尖峰（= fork / AOF rewrite）
redis-cli --intrinsic-latency 5     # 量「機器本身」的延遲（不經 Redis）；VM 上 > 100 µs 就是虛擬化層的問題

redis-cli latency doctor            # 需要 latency-monitor-threshold > 0；它會直接告訴你原因
redis-cli latency latest            # 各事件（command / fork / aof-write / expire-cycle）最近一次尖峰

redis-cli slowlog get 10            # 超過 slowlog-log-slower-than（10 ms）的指令
redis-cli slowlog reset

redis-cli info commandstats         # 每種指令的呼叫次數與平均耗時（usec_per_call）
redis-cli info stats | grep -E "latest_fork_usec|instantaneous_ops"
```

`INFO commandstats` 是最快找出「哪個指令在吃時間」的方法：

```
cmdstat_hgetall:calls=12034,usec=8420000,usec_per_call=699.7   ← 平均 0.7 ms，遠高於其他指令 → 有大 Hash
cmdstat_get:calls=9812345,usec=9812345,usec_per_call=1.0
```

---

## 2. 網路往返：Pipeline、批次指令、連線池

### Pipeline（Module 05）

一次送多個指令、一次收回覆。Redis 不需要任何設定，是 client 端的行為：

```bash
# redis-cli：從 stdin 讀指令，--pipe 用 pipeline 送
for i in $(seq 1 100000); do echo "SET key:$i $i"; done | redis-cli --pipe
# All data transferred. Waiting for the last reply... Last reply received from server. errors: 0, replies: 100000
```

Java（Lettuce / Spring Data Redis）：`redisTemplate.executePipelined(...)`；每批 100–1000 個指令是常見的甜蜜點——太大會讓 Redis 為回覆分配大量輸出緩衝。

### 批次指令

能用一個指令做完的，不要用 N 個：

| 不要 | 改用 |
|---|---|
| 迴圈 `GET k1`、`GET k2`… | `MGET k1 k2 …` |
| 迴圈 `HSET h f v` | `HSET h f1 v1 f2 v2 …`（一次多欄位） |
| 迴圈 `SADD s m` | `SADD s m1 m2 …` |
| 先 `GET` 再判斷再 `SET` | `SET … NX` / `INCR` / Lua（一次往返且原子） |

### Lua 腳本（Module 05）

把「讀 → 判斷 → 寫」壓成一次往返且原子。但 Lua 執行時整個 Redis 停住，所以：
- 只做幾個指令的組合，不做迴圈運算
- 超過 `busy-reply-threshold`（5 秒）其他 client 會收到 `BUSY`
- 用 `EVALSHA` 避免每次傳腳本本體

### 連線池

建立連線 = TCP 握手 + `AUTH` + `SELECT` + `CLIENT SETNAME`，比一個 `GET` 貴幾十倍。
Lettuce 預設是單一長連線多工（thread-safe），Jedis 需要 pool。`INFO stats` 的 `total_connections_received` 一直漲就是沒用連線池。

### Client-side caching（Redis 6+，RESP3）

`CLIENT TRACKING ON`：Redis 會在 key 被改動時通知 client 讓本地快取失效。適合「讀多、改少、熱點集中」的資料——熱 key 的終極解法。

---

## 3. 慢指令：O(N) 與阻塞

單執行緒的鐵律：**一個慢指令，所有人一起等**。

| 危險指令 | 為什麼 | 改用 |
|---|---|---|
| `KEYS pattern` | 掃全部 key，O(N) | `SCAN 0 MATCH pattern COUNT 100` 分批 |
| `SMEMBERS` / `HGETALL` / `LRANGE 0 -1` / `ZRANGE 0 -1` 對大集合 | 一次回傳全部 | `SSCAN` / `HSCAN` / `ZSCAN`，或 `LRANGE 0 99` 分頁 |
| `DEL bigkey` | 同步釋放百萬個元素 | `UNLINK bigkey`（背景釋放）；`lazyfree-lazy-user-del yes` 讓 DEL 自動變 UNLINK |
| `FLUSHALL` / `FLUSHDB` | 同步清空 | `FLUSHALL ASYNC` |
| `SORT` | O(N log N) | 應用端排序，或改用 Sorted Set |
| `SUNION` / `SINTER` / `ZUNIONSTORE` 對大集合 | O(N×M) | 預先計算、縮小集合 |
| `MONITOR` | 每個指令都複製一份給你 | 只在除錯短暫用；正式環境用 slowlog |
| `SAVE`（不是 BGSAVE） | 同步寫 RDB，整個停住 | `BGSAVE` |
| `DEBUG SLEEP` / `DEBUG RELOAD` | 顧名思義 | 不要 |
| 大量 key 同時過期 | 定期刪除迴圈變長 | TTL 加隨機抖動 |

`SCAN` 的正確用法（會有重複、但不會漏；`COUNT` 只是提示）：

```bash
redis-cli --scan --pattern 'session:*' | head        # 內建包裝
redis-cli --scan --pattern 'session:*' | xargs -n 100 redis-cli unlink   # 批次刪
```

---

## 4. 大 key 與熱 key

### 找出來

```bash
redis-cli --bigkeys                 # 用 SCAN 掃，各型別最大的 key（依元素數）
redis-cli --memkeys                 # 依實際記憶體（MEMORY USAGE）
redis-cli --hotkeys                 # 需要 maxmemory-policy 是 *lfu，依存取頻率
redis-cli memory usage user:1001    # 單一 key
redis-cli object freq user:1001     # LFU 計數（*lfu 策略下）
redis-cli debug object user:1001    # 編碼、序列化長度（serializedlength）
```

判斷標準（經驗值）：String > 10 KB、集合類 > 5000 個元素或 > 1 MB，就算大 key。

### 大 key 的處理

| 情況 | 做法 |
|---|---|
| 大 Hash / Set（百萬欄位） | 依 id 取模拆成 N 個：`user:1001:orders:{0..15}`，讀時算 bucket |
| 大 List 當佇列 | 改 Stream（有 consumer group、可 trim `MAXLEN ~ 10000`） |
| 大 String（JSON） | 壓縮（gzip / snappy 在應用端）；或改用 JSON 型別只讀寫需要的路徑 |
| 大 Sorted Set 排行榜 | 只保留前 N 名：`ZREMRANGEBYRANK lb 0 -1001` |
| 刪除 | `UNLINK`；集合類先 `SSCAN` 分批 `SREM` 再刪 |

### 熱 key 的處理

單一 key 承受大部分流量，Cluster 也救不了（它只會在一個節點上）：
1. **應用端本地快取**（Caffeine + 短 TTL，或 client-side caching）
2. **複製 N 份**：寫時 `key:0 … key:N-1` 都寫，讀時隨機挑一個——Cluster 會把它們分到不同節點
3. **讀寫分離**：讀走 Replica

---

## 5. 記憶體最佳化

### 看清楚記憶體去哪了

```bash
redis-cli info memory
# used_memory_human:1.2G           資料本身
# used_memory_rss_human:1.5G       OS 實際配置給行程的
# mem_fragmentation_ratio:1.25     rss / used；>1.5 碎片多，<1 用到 swap
# used_memory_peak_human           歷史高點
# used_memory_overhead             非資料的開銷（連線緩衝、複寫 backlog、dict 本身）
# used_memory_dataset              真正的資料
redis-cli memory stats              # 更細：keys.bytes-per-key、overhead.total
redis-cli memory doctor             # 它會直接給建議
```

### 減少開銷的手段（依效果排序）

1. **合併小 key 成 Hash**（listpack 編碼，[01 §3](01-architecture.md#3-記憶體模型一個-key-到底占多少)）：1 萬個 `user:N:name` String → 100 個各含 100 欄位的 Hash，可省 5–10 倍。前提是欄位數 ≤ `hash-max-listpack-entries`（128）且值 ≤ 64 bytes。
2. **縮短 key**：`user:1001:profile` 比 `application:user-service:user:1001:profile` 少 30 bytes × 千萬個 key = 300 MB。
3. **給 TTL**：沒有 TTL 的快取只會漲。
4. **選對 `maxmemory-policy`**：`allkeys-lfu` 對有明顯熱點的快取命中率比 LRU 高。
5. **壓縮大 value**：應用端 gzip；或把 JSON 的 key 名縮短。
6. **調編碼門檻**（小心）：`hash-max-listpack-entries 256` 讓更多 Hash 留在省記憶體的編碼，但 listpack 是 O(N) 掃描，欄位太多會變慢。
7. **碎片整理**：`activedefrag yes`（Redis 4+，jemalloc 才有效）。`mem_fragmentation_ratio > 1.5` 且 `used_memory` 大時才需要。
8. **32-bit 編譯**：資料 < 4 GB 時指標少一半——現在幾乎沒人用。

### 淘汰的觀察

```bash
redis-cli info stats | grep -E "evicted_keys|expired_keys|keyspace_hits|keyspace_misses"
```

`evicted_keys` 一直漲 = 記憶體不夠；命中率 = hits / (hits + misses)，快取低於 80% 就該檢查 TTL 與容量。

---

## 6. 持久化對延遲的影響

### fork

RDB bgsave 與 AOF rewrite 都要 fork。fork 的時間與**行程的頁表大小**成正比（不是資料大小）：

| 資料集 | fork 時間（無 THP、實體機） |
|---|---|
| 1 GB | ~10–20 ms |
| 10 GB | ~100–300 ms |
| 30 GB+ | 秒級 → 這麼大請拆成多個實例 |

fork 期間主執行緒是停的：`INFO stats` 的 `latest_fork_usec`。
VM 上 fork 更慢（EPT / 巢狀分頁）；**THP 開著會讓 fork 後每次寫入複製 2 MB 而不是 4 KB**，這就是一定要關 THP 的原因。

### AOF fsync

| `appendfsync` | 延遲影響 | 資料遺失 |
|---|---|---|
| `no` | 無（交給 OS，通常 30 秒） | 最多 30 秒 |
| `everysec` | 背景執行緒 fsync；磁碟慢時主執行緒最多被擋 2 秒 | 最多 1 秒 |
| `always` | 每個寫入都等 fsync，吞吐量掉 10–50 倍 | 0（理論上） |

`no-appendfsync-on-rewrite yes`：rewrite 期間不 fsync（避免磁碟 I/O 競爭），代價是那段時間可能丟 30 秒。
磁碟慢的機器上，`latency latest` 會看到 `aof-fsync-always` / `aof-write-pending-fsync` 事件。

### 把持久化移到 Replica

Master `save ""` + `appendonly no`，Replica 開持久化。Master 沒有 fork 尖峰，代價是 Master 重啟後是空的（要從 Replica 同步回來，且**絕不能**設定成自動重啟 + 自動變回 Master，否則會把 Replica 也清空）。

---

## 7. 多核、多實例、OS

### I/O threads（Redis 6+）

```conf
io-threads 4              # 建議 = 核心數 - 1，最多 8；4 核以下不用開
io-threads-do-reads yes
```

只加速網路讀寫，指令執行仍單執行緒。實測在高連線數、小 value 的場景約提升 1.5–2 倍；pipeline 已經開很大時效果有限。

### 多實例 / Cluster

一台 16 核機器跑一個 Redis 只用 1 核。要吃滿：跑 4–8 個實例（不同 port、各自 `maxmemory`），用 Cluster 組起來。每個實例 `maxmemory` ≤ 10–15 GB，fork 才不會太慢。

### OS（[03 §7](03-deploy-vm.md#7-os-調校) 有完整版）

- THP `never`、`vm.overcommit_memory=1`、`vm.swappiness=1`
- 把 Redis 綁在固定 CPU（`taskset`），並把網卡中斷綁到別的核心
- 關掉 NUMA 的自動 balancing 或用 `numactl --interleave=all`
- 網卡：關 `irqbalance` 對 Redis 核心的干擾；開 `net.ipv4.tcp_tw_reuse`
- 容器：`--network host` 省掉 bridge 的一層 NAT

### Client 端

- 應用與 Redis 放同一個可用區；跨區每次往返 +1–2 ms，等於把 Redis 變慢 10 倍
- 逾時設短（100–500 ms）並重試，不要讓一個慢的 Redis 拖垮整個服務
- 監控 client 端的 p99，Redis 端的指標看不到網路

---

## 8. 監控指標與告警門檻

| 指標（`INFO` / exporter） | 正常 | 告警 | 意義 |
|---|---|---|---|
| `used_memory / maxmemory` | < 80% | > 90% | 快淘汰 / 拒寫了 |
| `mem_fragmentation_ratio` | 1.0–1.5 | > 1.5 或 < 1 | 碎片 / swap |
| `evicted_keys` 增速 | 0 | 持續 > 0（非快取用途） | 記憶體不夠 |
| `keyspace_hits / (hits+misses)` | > 90%（快取） | < 80% | TTL 太短或容量不夠 |
| `instantaneous_ops_per_sec` | 依基準 | 接近單核天花板 | 要 pipeline / 分片 |
| `connected_clients` | 依連線池 | 接近 `maxclients` | 連線洩漏 |
| `rejected_connections` | 0 | > 0 | `maxclients` / ulimit |
| `latest_fork_usec` | < 100 ms | > 500 ms | 資料集太大或 THP |
| `rdb_last_bgsave_status` / `aof_last_bgrewrite_status` | ok | err | 磁碟 |
| `master_link_status`（Replica） | up | down | 複寫斷線 |
| `master_repl_offset - slave_repl_offset` | 小 | 持續增大 | Replica 追不上 |
| slowlog 筆數增速 | 0 | > 0 | 有慢指令 |
| `rdb_changes_since_last_save` | 依 save 設定 | 很大且很久沒存 | 快照失敗 |

Grafana 儀表板（`docker compose up -d`）已把這些都畫好。

---

## 9. 檢查清單

上線前逐條確認：

- [ ] 沒有 `KEYS`、`SMEMBERS`/`HGETALL` 大集合、`FLUSH*` 出現在應用程式碼（grep 一次）
- [ ] 批次操作用 `MGET` / pipeline；每批 ≤ 1000
- [ ] 連線池；逾時 ≤ 500 ms
- [ ] 所有快取 key 有 TTL，且有隨機抖動
- [ ] `--bigkeys` 沒有 > 1 MB 或 > 5000 元素的 key
- [ ] `maxmemory` 已設、策略正確、機器記憶體 ≥ maxmemory × 1.5
- [ ] THP never、overcommit 1、swappiness 1、日誌無 WARNING
- [ ] `latest_fork_usec` < 100 ms
- [ ] `slowlog-log-slower-than 10000` 並有人在看
- [ ] Grafana / 告警接上第 8 節的門檻
- [ ] 用 `redis-benchmark` 在正式機型上量過基準，知道天花板在哪
