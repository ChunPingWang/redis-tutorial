# 01 · Redis 架構設計：從內部原理到拓撲選型

> 這一章回答三個問題：**Redis 在裡面是怎麼動的**、**我的系統該用哪一種拓撲**、**要準備多少資源**。
> 讀完再看 Module 01–14 的程式碼，你會知道每個 API 背後付出的代價。

## 目錄

1. [Redis 是什麼、不是什麼](#1-redis-是什麼不是什麼)
2. [內部架構：單執行緒事件迴圈](#2-內部架構單執行緒事件迴圈)
3. [記憶體模型：一個 key 到底占多少](#3-記憶體模型一個-key-到底占多少)
4. [過期與淘汰：記憶體滿了怎麼辦](#4-過期與淘汰記憶體滿了怎麼辦)
5. [持久化架構：RDB、AOF、混合](#5-持久化架構rdbaof混合)
6. [四種拓撲：單機、主從、Sentinel、Cluster](#6-四種拓撲單機主從sentinelcluster)
7. [容量規劃](#7-容量規劃)
8. [Key 與資料建模原則](#8-key-與資料建模原則)
9. [設計反模式](#9-設計反模式)

---

## 1. Redis 是什麼、不是什麼

**Redis 是一個把資料放在記憶體裡的資料結構伺服器。**
三個關鍵詞決定了它的一切特性：

| 關鍵詞 | 帶來的好處 | 帶來的限制 |
|---|---|---|
| **記憶體** | 每個操作 µs 級延遲、單機十萬級 QPS | 資料量受 RAM 限制、比磁碟貴、重啟會丟（除非持久化） |
| **資料結構** | 不只是 key-value：List、Hash、Sorted Set、Stream…伺服器端直接操作 | 沒有 SQL、沒有 JOIN、沒有二級索引（除非用 Search 模組） |
| **單執行緒指令執行** | 每個指令天然原子、沒有鎖 | 一個慢指令會擋住所有人 |

Redis **不是**：
- 不是關聯式資料庫的替代品——它沒有交易隔離級別、沒有複雜查詢
- 不是「絕對不丟資料」的儲存——`appendfsync everysec` 最多丟 1 秒；主從複寫是非同步的
- 不是無限大的快取——`maxmemory` 到了就得淘汰或拒寫

> Redis 8 把過去獨立的 Stack 模組（JSON、Search、TimeSeries、Bloom 等機率結構、Vector Set）納入 Redis Open Source 本體，
> 授權改為 AGPLv3 / RSALv2 / SSPLv1 三選一。`redis:8` 映像檔內建這些模組，`redis:8-alpine` 只有核心。

---

## 2. 內部架構：單執行緒事件迴圈

```
                     ┌──────────────────────────────────────────────┐
  Client A ──┐       │              Redis 主執行緒                   │
  Client B ──┼──TCP──┤  epoll/kqueue 事件迴圈                        │
  Client C ──┘       │   ┌─────────┐  ┌──────────┐  ┌────────────┐  │
                     │   │ 讀請求   │→ │ 執行指令  │→ │ 寫回覆      │  │
                     │   └─────────┘  └──────────┘  └────────────┘  │
                     │        ↑          （唯一會碰資料的地方）   ↑    │
                     │   I/O threads（6.0+，可選）              I/O threads
                     └──────────────────────────────────────────────┘
                              │                    │
                   ┌──────────┴────────┐   ┌───────┴────────────┐
                   │  背景執行緒（bio） │   │  fork 出的子行程     │
                   │  · lazyfree 釋放   │   │  · RDB bgsave       │
                   │  · AOF fsync      │   │  · AOF rewrite      │
                   │  · 關閉檔案       │   │  （copy-on-write）   │
                   └───────────────────┘   └────────────────────┘
```

**為什麼單執行緒還這麼快？**
1. 資料在記憶體，沒有磁碟 I/O 等待
2. 事件迴圈用 I/O 多路複用，一個執行緒同時服務上萬連線
3. 沒有鎖、沒有 context switch，CPU cache 命中率高
4. 瓶頸通常是**網路往返**，不是 CPU——這就是 Pipeline 能快 6–10 倍的原因（見 [04 效能最佳化](04-performance-tuning.md)）

**單執行緒的代價**：一個 `KEYS *`、一個對百萬成員 Set 的 `SMEMBERS`、一個 `DEL` 大 key，
都會讓後面所有 client 排隊。所以 Redis 的效能規則只有一條：**每個指令都要快**。

**其實不只一個執行緒**（初學者常誤會）：
- **I/O threads（Redis 6.0+）**：只分擔「讀請求 / 寫回覆」的網路部分，指令執行仍是單執行緒。多核 + 高連線數才有用。
- **背景執行緒**：`UNLINK` / `FLUSHALL ASYNC` 的記憶體釋放、AOF 的 fsync、關閉大檔案。
- **fork 子行程**：RDB 快照與 AOF 重寫。靠 OS 的 copy-on-write，父行程繼續服務。
  fork 的代價與資料集大小成正比——10 GB 的實例 fork 可能要數百毫秒，這段時間主執行緒是停的（`INFO stats` 的 `latest_fork_usec`）。

**RESP 協定**：client 與 server 之間用的是純文字協定（RESP2 / RESP3），
`redis-cli`、任何語言的 client、甚至 `telnet` 都能講。這也是為什麼 Redis 極容易被各語言支援。

---

## 3. 記憶體模型：一個 key 到底占多少

每個 key 在 Redis 裡是：`dict entry → key (sds) → redisObject → 實際資料`。
即使 value 只是一個數字，**光是這層包裝就要約 50–70 bytes**。

```
key "user:1001:name" → redisObject{type=string, encoding=embstr, lru, refcount, ptr} → "Alice"
                       ────────────── 約 16 bytes ──────────────   + sds header + 資料
```

**同一個型別有多種內部編碼**，Redis 會依大小自動切換——這就是記憶體最佳化的核心：

| 型別 | 小資料的編碼（省記憶體） | 大資料的編碼（省時間） | 切換門檻（redis.conf 預設） |
|---|---|---|---|
| String | `int`（純整數）/ `embstr`（≤44 bytes） | `raw` | 44 bytes |
| Hash | `listpack` | `hashtable` | `hash-max-listpack-entries 128`、`hash-max-listpack-value 64` |
| List | `listpack` | `quicklist`（listpack 串起來） | `list-max-listpack-size -2`（每節點 8KB） |
| Set | `intset`（全整數）/ `listpack` | `hashtable` | `set-max-intset-entries 512`、`set-max-listpack-entries 128` |
| Sorted Set | `listpack` | `skiplist` + `hashtable` | `zset-max-listpack-entries 128`、`zset-max-listpack-value 64` |

實際意義：
- 把 100 個「小欄位」塞進一個 Hash（listpack 編碼），比存 100 個 String key 省 5–10 倍記憶體
- 但一旦超過門檻轉成 hashtable，優勢就消失；而且 listpack 是 O(N) 掃描，欄位太多會變慢
- 用 `OBJECT ENCODING key` 看編碼、`MEMORY USAGE key` 看實際位元組數（Module 06 資料建模的測試就在比這個）

```bash
redis-cli
> HSET small f1 v1 f2 v2
> OBJECT ENCODING small        # "listpack"
> MEMORY USAGE small           # 約 60–80 bytes
```

---

## 4. 過期與淘汰：記憶體滿了怎麼辦

**過期（expire）**：key 帶 TTL，到期就該消失。Redis 用兩種方式刪：
1. **惰性刪除**：有人存取這個 key 時發現過期 → 當場刪
2. **定期刪除**：每 100 ms（`hz 10`）隨機抽樣帶 TTL 的 key，過期的刪掉；若抽到的過期比例 > 25% 就再抽一輪

所以 `INFO keyspace` 的 `expires` 數量可能包含「已經過期但還沒被刪」的 key，這是正常的。

**淘汰（eviction）**：`used_memory` 達到 `maxmemory` 時，依 `maxmemory-policy` 決定：

| 策略 | 行為 | 適用 |
|---|---|---|
| `noeviction`（預設） | 寫入指令回 `OOM` 錯誤，讀取照常 | 資料不能丟（當資料庫用）——但要配監控 |
| `allkeys-lru` | 從所有 key 淘汰最久沒用的 | **純快取的標準選擇** |
| `allkeys-lfu` | 淘汰最少使用的（Redis 4+） | 有明顯熱點的快取 |
| `volatile-lru` / `volatile-lfu` | 只從有 TTL 的 key 淘汰 | 混合：一部分 key 是快取、一部分是持久資料 |
| `volatile-ttl` | 淘汰最快到期的 | 想讓「快到期的先走」 |
| `allkeys-random` / `volatile-random` | 隨機 | 幾乎不用 |

Redis 的 LRU 是**近似 LRU**：隨機抽 `maxmemory-samples`（預設 5）個 key 淘汰最舊的，
不是真的維護一條 LRU 鏈（省記憶體）。抽 10 個就很接近真 LRU 了。

> **一定要設 `maxmemory`**。不設的話 Redis 會一直吃到被 OS 的 OOM killer 殺掉——連淘汰的機會都沒有。
> 而且要留空間給 fork：正式環境 `maxmemory` 建議 ≤ 實體記憶體 60–70%（見第 7 節）。

---

## 5. 持久化架構：RDB、AOF、混合

```
  RDB（快照）                           AOF（追加日誌）
  ┌──────────┐  fork   ┌──────────┐     每個寫指令 → append 到 AOF 檔 → fsync
  │ 主行程    │ ──────→ │ 子行程    │      SET a 1
  │ 繼續服務  │         │ 寫 dump.rdb│     INCR b
  └──────────┘         └──────────┘      ...
  · 檔案小、載入快                      · 最多丟 1 秒（everysec）
  · 遺失最後一次快照後的資料             · 檔案大、載入慢 → 用 rewrite 壓縮
  · fork 有 CPU/記憶體代價              · rewrite 也要 fork

  混合持久化（aof-use-rdb-preamble yes，Redis 7 預設）
  appendonlydir/
  ├── appendonly.aof.1.base.rdb    ← rewrite 時產生的 RDB 格式快照（載入快）
  ├── appendonly.aof.1.incr.aof    ← 之後的增量指令（丟得少）
  └── appendonly.aof.manifest
```

| 需求 | 建議設定 |
|---|---|
| 純快取，丟了可以重建 | `save ""`（關 RDB）+ `appendonly no`，重啟後從資料庫重新暖機 |
| 一般應用（Session、計數、佇列） | `save 3600 1 300 100 60 10000` + `appendonly yes` + `appendfsync everysec`（本專案的預設） |
| 完全不能丟 | `appendfsync always`（每次寫都 fsync，吞吐量掉 10 倍以上）——通常寧可用主從 + `min-replicas-to-write` |

RPO / RTO 的具體計算在 Module 08 的測試裡有練習。

---

## 6. 四種拓撲：單機、主從、Sentinel、Cluster

```
 單機                主從（讀寫分離）          Sentinel（自動故障轉移）           Cluster（分片）
 ┌───┐               ┌───┐                    ┌───┐ ┌───┐ ┌───┐               ┌───┐ ┌───┐ ┌───┐
 │ M │               │ M │──┬──→│ R │         │ S │ │ S │ │ S │  監控 + 投票  │M1 │ │M2 │ │M3 │ slots
 └───┘               └───┘  └──→│ R │         └─┬─┘ └─┬─┘ └─┬─┘               └─┬─┘ └─┬─┘ └─┬─┘ 0-5460
                                                └──┬──┴──┬──┘                   │     │     │  5461-10922
                                                 ┌───┐  ┌───┐ ┌───┐            ┌───┐ ┌───┐ ┌───┐ 10923-16383
                                                 │ M │→ │ R │ │ R │            │R1 │ │R2 │ │R3 │
                                                 └───┘  └───┘ └───┘            └───┘ └───┘ └───┘
 練習環境：docker-compose.yml                  docker-compose-sentinel.yml     docker-compose-cluster.yml
```

| | 單機 | 主從 | Sentinel | Cluster |
|---|---|---|---|---|
| 解決什麼 | 最簡單 | 讀擴充、有備份 | **Master 掛了自動切換** | **資料量 / 寫入量超過一台機器** |
| 資料容量 | 一台 | 一台 | 一台 | 多台加總 |
| 寫入吞吐 | 一台 | 一台 | 一台 | 多台加總 |
| 故障切換 | 無 | 手動（`REPLICAOF NO ONE`） | 自動，秒級 | 自動，秒級 |
| 多 key 指令 | 都可以 | 都可以 | 都可以 | **只能同 slot**（hash tag `{...}`） |
| Client 複雜度 | 最低 | 低 | 中（要問 Sentinel 誰是 Master） | 高（要處理 MOVED / ASK） |
| 最少節點 | 1 | 2 | 3 Sentinel + 2 Redis | 6（3 主 3 從） |
| 對應模組 | M01–08 | M09 | M09 | M10 |

**決策樹**：

```
資料量會超過單機記憶體，或寫入 QPS 超過 10 萬？
├─ 是 → Cluster（先確認你的 key 設計能接受「多 key 指令只能同 slot」）
└─ 否 → Master 掛掉能接受人工介入嗎？
        ├─ 能（內部工具、快取） → 單機 + 持久化，或主從
        └─ 不能 → Sentinel（3 個 Sentinel 放在不同機器 / 可用區）
```

**Sentinel 運作**（Module 09）：
1. 每個 Sentinel 每秒 PING 所有 Master / Replica / 其他 Sentinel
2. `down-after-milliseconds` 內沒回 → 該 Sentinel 認為「主觀下線」（**SDOWN**）
3. 問其他 Sentinel，達到 `quorum` 個同意 → 「客觀下線」（**ODOWN**）
4. Sentinel 之間選一個 leader（Raft 式投票，需要**過半數** Sentinel，所以要奇數個且 ≥ 3）
5. leader 挑一個 Replica 升為 Master（優先序：`replica-priority` → 複寫 offset 最新 → run id）
6. 通知其他 Replica `REPLICAOF` 新 Master；舊 Master 回來後被降為 Replica

實測：本專案的 Sentinel 環境，停掉 Master 後 **約 6 秒**完成切換（`scripts/verify-sentinel.sh`）。

**Cluster 運作**（Module 10）：
- 16384 個 hash slot，`slot = CRC16(key) mod 16384`；每個 Master 負責一段
- key 裡有 `{...}` 時只對大括號內的部分做 hash（**hash tag**）→ `{user:1001}:profile` 與 `{user:1001}:cart` 同 slot，才能一起 `MGET` / 交易 / Lua
- Client 連到任一節點，若 key 不在這個節點會收到 `MOVED <slot> <ip:port>`，`redis-cli -c` 會自動跟過去
- 節點之間用 gossip（port + 10000 的 bus port）交換狀態；Master 掛了，它的 Replica 會發起選舉（需要過半數 Master 同意）
- 限制：只有 DB 0、多 key 指令要同 slot、`SELECT` 不能用、Pub/Sub 訊息要廣播到全叢集（7.0 的 Sharded Pub/Sub 解決）

---

## 7. 容量規劃

### 7.1 記憶體

```
所需記憶體 ≈ key 數 × (key 平均長度 + value 平均大小 + 約 60 bytes 包裝) × 1.2（碎片）
```

用真實資料驗證：塞 1 萬筆代表性資料，看 `INFO memory` 的 `used_memory`，乘上倍數。
`MEMORY USAGE key` 可以看單一 key。

**機器要比 `maxmemory` 大很多**：
- fork（RDB / AOF rewrite）期間，寫入越多 copy-on-write 複製的頁越多，極端情況要**兩倍**記憶體
- 複寫時 Master 端有 `client-output-buffer-limit replica` 的緩衝區
- OS 自己要用

| 實體記憶體 | 建議 `maxmemory` |
|---|---|
| 4 GB | 2.5 GB |
| 16 GB | 10 GB |
| 64 GB | 40 GB（這麼大建議改 Cluster 拆成多台；fork 太慢） |

### 7.2 CPU

Redis 指令執行只用**一個核心**。多核機器的選擇：
- 開 `io-threads 4`（網路部分多執行緒，最多提升約 2 倍）
- 一台機器跑多個 Redis 實例（不同 port），用 Cluster 組起來
- 監控 `INFO cpu` 的 `used_cpu_sys` / `used_cpu_user`；主執行緒滿載時 `instantaneous_ops_per_sec` 就是這台的天花板

### 7.3 網路

```
頻寬 ≈ QPS × (平均請求 + 平均回覆大小)
```
10 萬 QPS × 1 KB value = 800 Mbps，已經吃滿 1 Gbps 網卡。**大 value 的瓶頸永遠是網路**（見 04 的 benchmark：100 KB value 只剩 6k QPS）。

### 7.4 連線

- 每個連線約 10 KB 記憶體 + 一個 file descriptor
- `maxclients 10000` 要配合 `ulimit -n`（systemd 的 `LimitNOFILE`）
- 應用端用連線池，不要每個請求開連線（TCP 握手 + AUTH 的成本比指令本身高）

### 7.5 磁碟

- RDB 檔約等於 `used_memory` 的 30–50%（有壓縮）
- AOF 在 rewrite 前可能是 RDB 的 2–5 倍
- 預留 `maxmemory × 3` 的磁碟空間；SSD 對 `appendfsync everysec` 的延遲穩定性很有幫助

---

## 8. Key 與資料建模原則

本專案的慣例是 `service:entity:id[:field]`（`RedisKeyConvention`），理由：
- 冒號分隔在 RedisInsight 會顯示成樹狀，好瀏覽
- 前綴一致才方便 `SCAN MATCH order:*` 與 ACL 的 key pattern（`~order:*`）
- key 越短越省記憶體，但不要短到看不懂：`u:1001` 省不了幾個 bytes，`user:1001` 好維護得多

建模的核心取捨（Module 06）：

| 做法 | 例子 | 優點 | 缺點 |
|---|---|---|---|
| 一個 key 存整個物件（JSON 字串 / JSON 型別） | `user:1001` → `{"name":..,"email":..}` | 一次讀寫、原子 | 改一個欄位要重寫整個 |
| 一個 Hash 存物件的欄位 | `HSET user:1001 name Alice email ..` | 可以只讀寫單一欄位、省記憶體 | 欄位值只能是字串 |
| 拆成多個 key | `user:1001:name`、`user:1001:email` | 各欄位獨立 TTL | key 數暴增、包裝成本 |
| 二級索引 | `SADD users:by-city:taipei 1001` | 反向查詢 | 要自己維護一致性 |

**一定要決定每個 key 的生命週期**：它有 TTL 嗎？誰負責刪？沒有答案的 key 就是未來的記憶體洩漏。

---

## 9. 設計反模式

| 反模式 | 為什麼糟 | 改成 |
|---|---|---|
| `KEYS *` | O(N) 掃全部 key，百萬 key 時擋住所有人數秒 | `SCAN` 分批 |
| Big key（一個 List / Hash / Set 幾十萬個元素） | 讀取、刪除、複寫、rewrite 都會卡；Cluster 無法分片它 | 拆成多個 key（`user:1001:orders:2024-01`） |
| Hot key（一個 key 承受大部分流量） | 單一節點 CPU 打滿，Cluster 也救不了 | 應用端本地快取、複製多份（`key:1`…`key:N` 隨機讀） |
| 快取沒有 TTL | 記憶體只增不減 | 每個快取 key 都給 TTL，加隨機抖動避免同時過期 |
| 用 Redis 當唯一儲存 | 非同步複寫 + everysec 都可能丟資料 | 真相在資料庫，Redis 是加速層；或接受丟資料的風險並寫進 SLA |
| 大 value（> 100 KB） | 吃頻寬、擋住其他請求 | 壓縮、拆分、只快取需要的欄位 |
| 每個請求開新連線 | TCP + AUTH 的成本 > 指令 | 連線池 |
| 在交易 / Lua 裡做很多事 | Lua 執行時整個 Redis 停住（`BUSY`） | Lua 只做「幾個指令的原子組合」，不做迴圈運算 |
| 無密碼綁 0.0.0.0 | 幾分鐘內被掃到並植入挖礦 | `bind` 內網 + `requirepass` / ACL + 防火牆（見 [06](06-operations.md)） |

---

**下一步**：
- 想動手 → [02 容器佈署](02-deploy-container.md) 或 [03 VM 佈署](03-deploy-vm.md)
- 想知道為什麼慢 → [04 效能最佳化](04-performance-tuning.md)
- 想知道怎麼用 → [05 應用場景](05-use-cases.md)
