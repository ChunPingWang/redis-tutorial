# 01 · Redis 架構設計：從內部原理到拓撲選型

> 這一章回答三個問題：**Redis 在裡面是怎麼動的**、**我的系統該用哪一種拓撲**、**要準備多少資源**。
> 每個重點都先講「為什麼」——它解決什麼問題、不理它會出什麼事——再講「怎麼做」，最後才是指令與數字。
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

**這一節要解決的問題**：初學者最常見的錯誤不是「不會用 Redis」，而是「拿 Redis 做它不擅長的事」——把它當資料庫存唯一的一份資料、當成無限大的快取、期待它永不丟資料。後面每一章的取捨都建立在「Redis 到底是什麼」這個前提上，所以先把邊界畫清楚。

**Redis 是一個把資料放在記憶體裡的資料結構伺服器。** 這句話裡的三個關鍵詞，各自帶來一組好處與一組限制，而且好處與限制是一體兩面、不能只要一半：

| 關鍵詞 | 為什麼帶來好處 | 同一個原因造成的限制 |
|---|---|---|
| **記憶體** | 沒有磁碟 I/O，所以每個操作是 µs 級延遲、單機十萬級 QPS | 資料量受 RAM 限制、每 GB 比磁碟貴一個數量級、重啟就消失（除非持久化） |
| **資料結構** | 伺服器端直接操作 List、Hash、Sorted Set、Stream，不用把整包資料拉到應用端再算 | 沒有 SQL、沒有 JOIN、沒有二級索引（除非用 Search 模組）——查詢方式要在設計 key 時就決定 |
| **單執行緒指令執行** | 每個指令天然原子、沒有鎖、沒有競態 | 一個慢指令會擋住所有人 |

由此推出 Redis **不是**什麼：
- **不是關聯式資料庫的替代品**——它沒有交易隔離級別、沒有複雜查詢；需要「依任意條件找資料」就不該只靠 Redis。
- **不是「絕對不丟資料」的儲存**——`appendfsync everysec` 最多丟 1 秒；主從複寫是非同步的（第 5、6 節）。真相應該在資料庫，Redis 是加速層。
- **不是無限大的快取**——`maxmemory` 到了就得淘汰或拒寫（第 4 節），而且淘汰是有代價的。

> **Redis 8 的變化**：過去要另外安裝的 Stack 模組（JSON、Search、TimeSeries、Bloom 等機率結構、Vector Set）納入 Redis Open Source 本體，授權改為 AGPLv3 / RSALv2 / SSPLv1 三選一。這影響你選映像檔：`redis:8` 內建這些模組，`redis:8-alpine` 只有核心——Module 03、11、12、14 需要前者。

---

## 2. 內部架構：單執行緒事件迴圈

**這一節要解決的問題**：「單執行緒」是 Redis 效能規則的根源。不理解它，你會寫出 `KEYS *`、一次拉百萬元素的 `SMEMBERS`、用 `DEL` 刪大 key，然後在正式環境看到所有 client 一起卡住卻不知道為什麼。理解它之後，[04 效能最佳化](04-performance-tuning.md) 的每一條規則都變成常識。

### 為什麼單執行緒反而快

直覺上多執行緒才快，但 Redis 的工作特性讓單執行緒成為更好的選擇：資料在記憶體，一個指令的執行時間通常只有幾微秒，**瓶頸根本不在 CPU，而在網路往返與系統呼叫**。多執行緒帶來的鎖、context switch、CPU cache 失效，在這種工作負載下的開銷比收益大。所以 Redis 選擇：

1. 資料在記憶體 → 沒有磁碟 I/O 等待
2. 事件迴圈用 I/O 多路複用（epoll / kqueue）→ 一個執行緒同時服務上萬連線
3. 沒有鎖、沒有 context switch → CPU cache 命中率高
4. 瓶頸在網路往返 → 這正是 Pipeline 能快 6–10 倍的原因（[04 §2](04-performance-tuning.md#2-網路往返pipeline批次指令連線池)）

### 它實際上長什麼樣

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

「執行指令」這一格是唯一會碰資料的地方，而且一次只做一件事。所有 client 的指令排成一列依序通過它——這就是為什麼 `INCR` 不需要鎖也不會算錯（Module 01 的帳戶餘額就靠這個）。

### 單執行緒的代價：一個慢指令，所有人一起等

因為只有一條隊伍，一個 `KEYS *`、一個對百萬成員 Set 的 `SMEMBERS`、一個 `DEL` 大 key，都會讓後面所有 client 排隊等它做完。Redis 的效能規則因此只有一條：**每個指令都要快**。什麼指令慢、怎麼換掉，見 [04 §3](04-performance-tuning.md#3-慢指令on-與阻塞)。

### 其實不只一個執行緒——為什麼要知道這件事

初學者常以為 Redis 全部只有一個執行緒，於是把 `latest_fork_usec` 的延遲尖峰、`UNLINK` 與 `DEL` 的差別都解釋不通。實際上有三種「不在主執行緒上」的工作，各自對應一個你會遇到的現象：

- **I/O threads（Redis 6.0+）**：只分擔「讀請求 / 寫回覆」的網路部分，指令執行仍是單執行緒。所以它只在「多核、高連線數、網路處理占比高」時有用，而且最多提升約 2 倍（[04 §7](04-performance-tuning.md#7-多核多實例os)）。
- **背景執行緒（bio）**：`UNLINK` / `FLUSHALL ASYNC` 的記憶體釋放、AOF 的 fsync、關閉大檔案。這就是為什麼刪大 key 要用 `UNLINK` 而不是 `DEL`——前者把「釋放記憶體」丟給背景執行緒，主執行緒不用等。
- **fork 出的子行程**：RDB 快照與 AOF 重寫。靠 OS 的 copy-on-write，父行程繼續服務。但 fork 本身的代價與資料集大小成正比——10 GB 的實例 fork 可能要數百毫秒，**這段時間主執行緒是停的**（`INFO stats` 的 `latest_fork_usec`）。這是「大實例要拆」的根本原因（第 7 節）。

### RESP 協定：為什麼任何語言都能用 Redis

client 與 server 之間用的是純文字協定（RESP2 / RESP3），`redis-cli`、任何語言的 client、甚至 `telnet` 都能講。這解釋了兩件事：為什麼 Redis 幾乎每個語言都有成熟的 client；以及為什麼 Pipeline 不需要伺服器端任何設定——它只是 client 把多個請求連續寫進同一條 TCP 連線。

---

## 3. 記憶體模型：一個 key 到底占多少

**這一節要解決的問題**：記憶體是 Redis 唯一真正稀缺的資源（第 1 節），而初學者估算記憶體時常常只算「value 有多大」，結果實際用量是估算的 3–5 倍。原因是每個 key 都有一層固定的包裝成本，而且同一種型別在不同大小下用的內部結構不同。搞懂這兩件事，才能做第 7 節的容量規劃與 [04 §5](04-performance-tuning.md#5-記憶體最佳化) 的記憶體最佳化。

### 包裝成本：為什麼 1 萬個小 key 比想像中貴

每個 key 在 Redis 裡不是「key 字串 + value 字串」而已，而是一串結構：`dict entry → key (sds) → redisObject → 實際資料`。即使 value 只是一個數字，**光是這層包裝就要約 50–70 bytes**。

```
key "user:1001:name" → redisObject{type=string, encoding=embstr, lru, refcount, ptr} → "Alice"
                       ────────────── 約 16 bytes ──────────────   + sds header + 資料
```

所以「1 億個 value 只有 8 bytes 的計數器」不是 800 MB，而是接近 6–7 GB。這也是第 8 節「把小 key 合併成 Hash」的動機。

### 內部編碼：為什麼同一個型別有時省記憶體、有時快

Redis 面對的是兩種互相衝突的需求：小資料要省記憶體，大資料要操作快。它的解法是**同一個型別準備多種內部編碼，依大小自動切換**——小的時候用緊湊、連續的結構（省記憶體但 O(N) 掃描），超過門檻就轉成雜湊表 / 跳躍表（快但每個元素有指標開銷）。

| 型別 | 小資料的編碼（省記憶體） | 大資料的編碼（省時間） | 切換門檻（redis.conf 預設） |
|---|---|---|---|
| String | `int`（純整數）/ `embstr`（≤44 bytes） | `raw` | 44 bytes |
| Hash | `listpack` | `hashtable` | `hash-max-listpack-entries 128`、`hash-max-listpack-value 64` |
| List | `listpack` | `quicklist`（listpack 串起來） | `list-max-listpack-size -2`（每節點 8KB） |
| Set | `intset`（全整數）/ `listpack` | `hashtable` | `set-max-intset-entries 512`、`set-max-listpack-entries 128` |
| Sorted Set | `listpack` | `skiplist` + `hashtable` | `zset-max-listpack-entries 128`、`zset-max-listpack-value 64` |

這張表對設計的實際意義：
- 把 100 個「小欄位」塞進一個 Hash（listpack 編碼），比存 100 個 String key 省 5–10 倍記憶體——因為 100 個包裝變成 1 個。
- 但一旦超過門檻轉成 hashtable，優勢就消失；而且 listpack 是 O(N) 掃描，欄位太多會變慢。所以「合併成 Hash」有上限，不是越大越好。
- 編碼切換是單向的：轉成 hashtable 之後即使元素刪到只剩一個，也不會轉回 listpack。

### 怎麼親眼確認

設計時不要用猜的。用 `OBJECT ENCODING` 看目前是哪種編碼、`MEMORY USAGE` 看實際位元組數（Module 06 資料建模的測試就在比這個）：

```bash
redis-cli
> HSET small f1 v1 f2 v2
> OBJECT ENCODING small        # "listpack"
> MEMORY USAGE small           # 約 60–80 bytes
```

---

## 4. 過期與淘汰：記憶體滿了怎麼辦

**這一節要解決的問題**：記憶體有限，資料只會越放越多，所以「舊資料怎麼消失」必須在設計時就決定，否則結果只有一種——Redis 被 OS 的 OOM killer 殺掉，連日誌都來不及寫。Redis 提供兩種讓資料消失的機制：**過期**（你告訴它什麼時候該消失）與**淘汰**（記憶體滿了它自己決定誰先走）。兩者的行為都跟直覺有出入。

### 過期：為什麼 key 過期了還在

你給 key 設了 TTL，直覺上到期那一刻它就該不見。但如果 Redis 為每個 key 設一個計時器，百萬個 key 就有百萬個計時器，主執行緒會被計時器淹沒。所以 Redis 用兩種「便宜」的方式刪：

1. **惰性刪除**：有人存取這個 key 時才檢查，發現過期就當場刪——沒人碰的 key 不花任何 CPU。
2. **定期刪除**：每 100 ms（`hz 10`）隨機抽樣帶 TTL 的 key，過期的刪掉；若抽到的過期比例 > 25% 就再抽一輪，直到比例降下來或用完時間片。

結果是：`INFO keyspace` 的 `expires` 數量可能包含「已經過期但還沒被刪」的 key，這是正常的，不是 bug。反過來的風險是：**大量 key 在同一秒過期**會讓定期刪除迴圈變長、主執行緒卡頓——這就是 [05 §1](05-use-cases.md#1-快取cache-aside) 快取雪崩要對 TTL 加隨機抖動的原因。

### 淘汰：為什麼一定要設 maxmemory

不設 `maxmemory` 的 Redis 會一直吃記憶體直到被 OS 殺掉，**連淘汰的機會都沒有**。設了之後，`used_memory` 達到上限時 Redis 才有機會依 `maxmemory-policy` 做選擇。而策略的選擇取決於一個問題：**你的資料丟了會怎樣？**

| 策略 | 行為 | 為什麼選它 |
|---|---|---|
| `noeviction`（預設） | 寫入指令回 `OOM` 錯誤，讀取照常 | 資料不能丟（當資料庫用）：寧可拒寫也不能默默刪——但一定要配監控，否則應用會突然全部寫入失敗 |
| `allkeys-lru` | 從所有 key 淘汰最久沒用的 | **純快取的標準選擇**：快取本來就可以重建，最久沒用的最不值得留 |
| `allkeys-lfu` | 淘汰最少使用的（Redis 4+） | 有明顯熱點的快取：LRU 會被「偶爾掃一次的大量冷 key」洗掉熱 key，LFU 看頻率不看時間 |
| `volatile-lru` / `volatile-lfu` | 只從有 TTL 的 key 淘汰 | 混合用途：有 TTL 的是快取可以丟，沒 TTL 的是持久資料不能碰 |
| `volatile-ttl` | 淘汰最快到期的 | 想讓「反正快過期的先走」 |
| `allkeys-random` / `volatile-random` | 隨機 | 幾乎不用；只在 key 的存取完全均勻時與 LRU 等價 |

### 為什麼 Redis 的 LRU 是「近似」的

真正的 LRU 要維護一條全域鏈結串列，每個 key 多兩個指標、每次存取都要搬動——在千萬 key 下記憶體與 CPU 都划不來。Redis 改成隨機抽 `maxmemory-samples`（預設 5）個 key 淘汰最舊的。抽 5 個已經很接近，抽 10 個幾乎等於真 LRU，代價是每次淘汰多掃幾個 key。

### 為什麼 maxmemory 不能設到實體記憶體上限

因為 Redis 的記憶體用量不只 `maxmemory` 管的資料本身：fork 做 RDB / AOF rewrite 時 copy-on-write 會複製被改動的頁、複寫時 Master 端有輸出緩衝區、OS 自己也要用。正式環境 `maxmemory` 建議 ≤ 實體記憶體 60–70%，具體算法在第 7 節。

---

## 5. 持久化架構：RDB、AOF、混合

**這一節要解決的問題**：Redis 資料在記憶體，行程一死就全沒了。持久化就是「把記憶體裡的東西寫到磁碟，重啟後能載回來」。但寫磁碟一定有代價——要嘛慢、要嘛占空間、要嘛有 fork 尖峰——所以 Redis 提供兩種機制讓你選擇要付哪一種代價。選錯的後果是：以為不會丟的資料丟了，或者為了不丟資料把吞吐量砍掉 10 倍。

### 兩種機制各自的取捨

```
  RDB（快照）                           AOF（追加日誌）
  ┌──────────┐  fork   ┌──────────┐     每個寫指令 → append 到 AOF 檔 → fsync
  │ 主行程    │ ──────→ │ 子行程    │      SET a 1
  │ 繼續服務  │         │ 寫 dump.rdb│     INCR b
  └──────────┘         └──────────┘      ...
  · 檔案小、載入快                      · 最多丟 1 秒（everysec）
  · 遺失最後一次快照後的資料             · 檔案大、載入慢 → 用 rewrite 壓縮
  · fork 有 CPU/記憶體代價              · rewrite 也要 fork
```

**RDB** 是某個時間點的完整快照。它靠 fork 子行程來寫（第 2 節），所以主行程不用停下來等磁碟，但代價是：fork 本身的停頓、copy-on-write 的額外記憶體，以及**兩次快照之間的資料全部會丟**。適合「備份」與「重啟後快速載入」。

**AOF** 把每一個寫指令追加到檔案。因為是逐筆記錄，資料遺失窗口可以縮到 1 秒（`everysec`）甚至 0（`always`），但檔案會一直長大、重啟時要重播每一條指令所以載入慢。Redis 用 **rewrite** 解決檔案膨脹：fork 一個子行程，把目前的資料集重新寫成最精簡的指令序列——所以 AOF 也逃不掉 fork 的代價。

### 為什麼現在預設是混合模式

既然 RDB 載入快、AOF 丟得少，Redis 4.0 起把兩者合起來：rewrite 時先以 RDB 格式寫一份快照當「基底」，之後的增量指令再用 AOF 格式追加。重啟時先載 RDB 部分（快），再重播增量（少）。Redis 7 起這是預設（`aof-use-rdb-preamble yes`），檔案結構如下：

```
  appendonlydir/
  ├── appendonly.aof.1.base.rdb    ← rewrite 時產生的 RDB 格式快照（載入快）
  ├── appendonly.aof.1.incr.aof    ← 之後的增量指令（丟得少）
  └── appendonly.aof.manifest
```

### 怎麼依需求選

決定因素只有一個：**這些資料丟了，你能不能從別的地方重建？**

| 需求 | 為什麼這樣設 | 建議設定 |
|---|---|---|
| 純快取，丟了可以從資料庫重建 | 持久化只會拖慢它，重啟後暖機就好 | `save ""`（關 RDB）+ `appendonly no` |
| 一般應用（Session、計數、佇列） | 丟 1 秒可接受，但不能丟幾分鐘 | `save 3600 1 300 100 60 10000` + `appendonly yes` + `appendfsync everysec`（本專案的預設） |
| 完全不能丟 | `always` 每次寫都等 fsync，吞吐量掉 10 倍以上——通常寧可用主從 + `min-replicas-to-write` 用多一份副本換速度 | `appendfsync always`，或改走第 6 節的複寫 |

RPO / RTO 的具體計算在 Module 08 的測試裡有練習；備份與還原的操作在 [06 §5](06-operations.md#5-備份與還原)。

---

## 6. 四種拓撲：單機、主從、Sentinel、Cluster

**這一節要解決的問題**：一台 Redis 有三個天花板——它會掛（可用性）、它的記憶體有限（容量）、它只用一個核心（寫入吞吐）。四種拓撲各自突破其中幾個天花板，但每多突破一個，架構與 client 的複雜度就多一層。選太簡單會在半夜被叫起來手動切換；選太複雜會在 key 設計上處處受限。這一節給你比較表與決策樹，讓你依「哪個天花板先到」來選。

### 四種拓撲長什麼樣

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

- **單機**：什麼天花板都沒突破，但最簡單。
- **主從**：多了副本，讀可以分散到 Replica、Master 壞了還有一份資料。但 Master 掛了要**人**去把 Replica 升級並改應用設定——複寫本身不會自動切換。
- **Sentinel**：在主從之上加一組「監督者」，替你做「發現 Master 掛了 → 選一個 Replica 升級 → 通知大家」。解決可用性，**不**解決容量與寫入吞吐（資料仍然全部在一台 Master 上）。
- **Cluster**：把 key 空間切成 16384 個 slot 分給多個 Master，每個 Master 各有 Replica。同時突破容量、寫入吞吐、可用性三個天花板，代價是「多 key 操作只能在同一個 slot」，key 設計從此受限。

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

### 決策樹：依「哪個天花板先到」選

先問容量與寫入（因為只有 Cluster 能解），再問可用性：

```
資料量會超過單機記憶體，或寫入 QPS 超過 10 萬？
├─ 是 → Cluster（先確認你的 key 設計能接受「多 key 指令只能同 slot」）
└─ 否 → Master 掛掉能接受人工介入嗎？
        ├─ 能（內部工具、快取） → 單機 + 持久化，或主從
        └─ 不能 → Sentinel（3 個 Sentinel 放在不同機器 / 可用區）
```

### Sentinel 怎麼運作（Module 09）

為什麼需要「一組」Sentinel 而不是一個？因為單一監督者自己也可能掛、也可能因為網路分割**誤判** Master 死了。所以 Sentinel 的每一步都設計成「多個獨立觀察者投票」：

1. 每個 Sentinel 每秒 PING 所有 Master / Replica / 其他 Sentinel。
2. `down-after-milliseconds` 內沒回 → 該 Sentinel **自己**認為「主觀下線」（**SDOWN**）——這只是一個人的意見。
3. 問其他 Sentinel，達到 `quorum` 個同意 → 「客觀下線」（**ODOWN**）——夠多人同意才算真的掛了，避免單一 Sentinel 的網路問題觸發切換。
4. Sentinel 之間選一個 leader（Raft 式投票）。投票需要**過半數** Sentinel——這就是為什麼要奇數個且 ≥ 3：兩個 Sentinel 無法形成「過半」，一個掛了另一個永遠選不出 leader。
5. leader 挑一個 Replica 升為 Master，優先序是：`replica-priority` 小的 → 複寫 offset 最新的（資料最完整）→ run id 最小的（純粹決勝）。
6. 通知其他 Replica `REPLICAOF` 新 Master；舊 Master 回來後也被降為 Replica——避免它帶著舊資料繼續接受寫入（腦裂）。

實測：本專案的 Sentinel 環境，停掉 Master 後 **約 7 秒**完成切換（`scripts/verify-sentinel.sh`）。容器環境有一個會讓這整套機制失效的坑（用主機名稱而不是 IP），見 [02 §3](02-deploy-container.md#3-sentinel-環境docker-compose-sentinelyml)。

### Cluster 怎麼運作（Module 10）

為什麼是「slot」而不是直接對 key 做 hash 分給節點？因為節點會增減。如果 key 直接對「節點數」取模，加一台機器就要搬動幾乎所有 key；改成先對固定的 16384 個 slot 取模、再把 slot 分配給節點，加機器只需要搬動一部分 slot，而且搬動期間服務不中斷。

- 16384 個 hash slot，`slot = CRC16(key) mod 16384`；每個 Master 負責一段。
- **hash tag**：key 裡有 `{...}` 時只對大括號內的部分做 hash。這是為了讓你能刻意把相關的 key 放在同一個 slot——`{user:1001}:profile` 與 `{user:1001}:cart` 同 slot，才能一起 `MGET` / 交易 / Lua。沒有 hash tag 的多 key 指令會收到 `CROSSSLOT` 錯誤。
- **重新導向**：Client 連到任一節點；若 key 不在這個節點會收到 `MOVED <slot> <ip:port>`。這樣 client 不需要事先知道全部拓撲，`redis-cli -c` 與 cluster-aware client 會自動跟過去並記住。
- 節點之間用 gossip（port + 10000 的 bus port）交換狀態，所以 bus port 也要開通；Master 掛了，它的 Replica 會發起選舉，需要**過半數 Master** 同意——同樣是為了避免網路分割時兩邊各自選出 Master。
- 限制的來源都是「資料被切開了」：只有 DB 0（`SELECT` 不能用）、多 key 指令要同 slot、Pub/Sub 訊息要廣播到全叢集（7.0 的 Sharded Pub/Sub 才解決）。

實測：停掉一個 Master 後 **約 9 秒** Replica 升級、`cluster_state:ok`（`scripts/verify-cluster.sh`）。

---

## 7. 容量規劃

**這一節要解決的問題**：Redis 上線後最常見的兩種事故是「記憶體不夠被 OOM 殺掉」和「網卡被大 value 打滿」，兩者都能在上線前用簡單的算術避開。但要算對，必須把第 3 節的包裝成本、第 2 節的 fork、第 6 節的複寫緩衝都算進去，而不是只算資料本身。

### 7.1 記憶體

為什麼不能只算 value 大小：第 3 節說過每個 key 有約 60 bytes 包裝，而且 jemalloc 分配記憶體時有碎片（`mem_fragmentation_ratio` 正常 1.0–1.5）。所以估算公式是：

```
所需記憶體 ≈ key 數 × (key 平均長度 + value 平均大小 + 約 60 bytes 包裝) × 1.2（碎片）
```

公式只能給數量級。真正的做法是**用真實資料驗證**：塞 1 萬筆代表性資料，看 `INFO memory` 的 `used_memory`，乘上倍數；單一 key 用 `MEMORY USAGE key` 看。

為什麼機器要比 `maxmemory` 大很多（第 4 節留下的問題）：
- fork（RDB / AOF rewrite）期間，父行程每改一頁記憶體，OS 就複製一頁給子行程。寫入越多複製越多，極端情況要**兩倍**記憶體。
- 複寫時 Master 端有 `client-output-buffer-limit replica` 的緩衝區，全量同步期間會堆積。
- OS 自己要用。

| 實體記憶體 | 建議 `maxmemory` | 理由 |
|---|---|---|
| 4 GB | 2.5 GB | 留 1.5 GB 給 fork 與 OS |
| 16 GB | 10 GB | 同上比例 |
| 64 GB | 40 GB | 數字上可以，但這麼大的實例 fork 太慢（第 2 節）；建議改 Cluster 拆成多台 |

### 7.2 CPU

為什麼多核機器不會讓 Redis 變快：指令執行只用**一個核心**（第 2 節）。所以買 32 核機器跑一個 Redis 是浪費。要吃滿多核，選擇是：
- 開 `io-threads 4`（網路部分多執行緒，最多提升約 2 倍）
- 一台機器跑多個 Redis 實例（不同 port），用 Cluster 組起來

怎麼知道天花板到了：監控 `INFO cpu` 的 `used_cpu_sys` / `used_cpu_user`；主執行緒滿載時 `instantaneous_ops_per_sec` 就是這台的極限，再多的 client 只會讓延遲變長。

### 7.3 網路

為什麼要算頻寬：Redis 本身每秒能處理十幾萬個指令，但每個指令的回覆都要經過網卡。value 一大，網卡比 CPU 先滿：

```
頻寬 ≈ QPS × (平均請求 + 平均回覆大小)
```

10 萬 QPS × 1 KB value = 800 Mbps，已經吃滿 1 Gbps 網卡。**大 value 的瓶頸永遠是網路**——[04 §1](04-performance-tuning.md#1-先量測benchmark-與延遲工具) 的實測裡 100 KB value 只剩 6k QPS，就是這個原因。

### 7.4 連線

為什麼連線數也要規劃：每個連線約 10 KB 記憶體 + 一個 file descriptor，而 Linux 預設每個行程只能開 1024 個檔案。連線一多就會看到 `max number of clients reached`，而且是在流量高峰時才發生。

- `maxclients 10000` 要配合 `ulimit -n`（systemd 的 `LimitNOFILE`，[03 §6](03-deploy-vm.md#6-systemd-服務)）
- 應用端用連線池，不要每個請求開連線——TCP 握手 + AUTH 的成本比指令本身高幾十倍

### 7.5 磁碟

為什麼記憶體資料庫還要算磁碟：持久化檔案在 rewrite 期間會同時存在新舊兩份，磁碟滿了 bgsave 失敗，Redis 預設會**拒絕所有寫入**（`MISCONF`）。

- RDB 檔約等於 `used_memory` 的 30–50%（有壓縮）
- AOF 在 rewrite 前可能是 RDB 的 2–5 倍
- 預留 `maxmemory × 3` 的磁碟空間；SSD 對 `appendfsync everysec` 的延遲穩定性很有幫助（慢磁碟會讓 fsync 卡住主執行緒，[04 §6](04-performance-tuning.md#6-持久化對延遲的影響)）

---

## 8. Key 與資料建模原則

**這一節要解決的問題**：Redis 沒有 schema、沒有查詢語言，「怎麼查」完全由「key 怎麼命名、資料怎麼拆」決定，而且事後很難改（改 key 格式等於搬遷全部資料）。所以建模的決定要在寫第一行程式之前做，這一節給你判斷的依據。

### 命名慣例：為什麼是 `service:entity:id`

本專案的慣例是 `service:entity:id[:field]`（`RedisKeyConvention`）。不是美觀問題，每一段都有功能：
- 冒號分隔在 RedisInsight 會顯示成樹狀，好瀏覽、好排查
- 前綴一致才能用 `SCAN MATCH order:*` 找出一類 key，也才能用 ACL 的 key pattern（`~order:*`）限制每個應用只能碰自己的資料（[06 §2](06-operations.md#2-acl最小權限帳號)）
- key 越短越省記憶體（第 3 節），但不要短到看不懂：`u:1001` 省不了幾個 bytes，`user:1001` 好維護得多

### 建模的核心取捨（Module 06）

四種做法沒有絕對的好壞，差別在「你最常做的操作是什麼」：

| 做法 | 例子 | 為什麼選它 | 代價 |
|---|---|---|---|
| 一個 key 存整個物件（JSON 字串 / JSON 型別） | `user:1001` → `{"name":..,"email":..}` | 一次讀寫、天然原子；讀取時總是要整個物件 | 改一個欄位要重寫整個 value（JSON 型別可以只改路徑） |
| 一個 Hash 存物件的欄位 | `HSET user:1001 name Alice email ..` | 可以只讀寫單一欄位；小物件用 listpack 編碼省記憶體（第 3 節） | 欄位值只能是字串，不能巢狀 |
| 拆成多個 key | `user:1001:name`、`user:1001:email` | 各欄位可以有獨立 TTL | key 數暴增、每個 key 都付包裝成本 |
| 二級索引 | `SADD users:by-city:taipei 1001` | Redis 沒有「依欄位查」，要反向查詢只能自己建索引 | 主資料與索引的一致性要自己維護（改 city 時兩邊都要改） |

### 一定要決定每個 key 的生命週期

為什麼：記憶體是有限的（第 4 節），而 Redis 不會替你記得「這個 key 是誰放的、什麼時候可以刪」。設計每個 key 時就要回答：它有 TTL 嗎？沒有的話誰負責刪？什麼時候？沒有答案的 key 就是未來的記憶體洩漏——而且是等到記憶體滿、開始淘汰或拒寫時才會被發現。

---

## 9. 設計反模式

**這一節要解決的問題**：下面每一條都是「當下看起來能動、上線後某天突然出事」的寫法，而且出事的原因都能從前面幾節推出來。把它當成 code review 的清單。

| 反模式 | 為什麼糟（對應章節） | 改成 |
|---|---|---|
| `KEYS *` | O(N) 掃全部 key，百萬 key 時擋住所有人數秒（§2 單執行緒） | `SCAN` 分批 |
| Big key（一個 List / Hash / Set 幾十萬個元素） | 讀取、刪除、複寫、rewrite 都會卡住主執行緒（§2）；Cluster 無法把一個 key 再分片（§6） | 拆成多個 key（`user:1001:orders:2024-01`） |
| Hot key（一個 key 承受大部分流量） | 單一節點 CPU 打滿（§7.2），Cluster 也救不了——它只會在一個節點上 | 應用端本地快取、複製多份（`key:1`…`key:N` 隨機讀） |
| 快取沒有 TTL | 記憶體只增不減（§4、§8） | 每個快取 key 都給 TTL，加隨機抖動避免同時過期 |
| 用 Redis 當唯一儲存 | 非同步複寫 + everysec 都可能丟資料（§1、§5） | 真相在資料庫，Redis 是加速層；或接受丟資料的風險並寫進 SLA |
| 大 value（> 100 KB） | 吃頻寬、擋住其他請求的回覆（§7.3） | 壓縮、拆分、只快取需要的欄位 |
| 每個請求開新連線 | TCP + AUTH 的成本 > 指令本身（§7.4） | 連線池 |
| 在交易 / Lua 裡做很多事 | Lua 執行時整個 Redis 停住，其他人收到 `BUSY`（§2） | Lua 只做「幾個指令的原子組合」，不做迴圈運算 |
| 無密碼綁 0.0.0.0 | 幾分鐘內被掃到並植入挖礦 | `bind` 內網 + `requirepass` / ACL + 防火牆（見 [06](06-operations.md)） |

---

**下一步**：
- 想動手 → [02 容器佈署](02-deploy-container.md) 或 [03 VM 佈署](03-deploy-vm.md)
- 想知道為什麼慢 → [04 效能最佳化](04-performance-tuning.md)
- 想知道怎麼用 → [05 應用場景](05-use-cases.md)
