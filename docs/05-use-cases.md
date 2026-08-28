# 05 · 應用場景：用 redis-cli 走一遍 14 個經典模式

> 每個場景都是「問題 → 用哪個資料結構 → 指令走一遍 → 坑在哪 → 對應的 Java 模組」。
> 指令全部可以貼進 `redis-cli`（或 RedisInsight 的 Workbench）直接執行；`scripts/smoke-test.sh` 會自動跑其中的關鍵斷言。

## 選型速查表

| 需求 | 資料結構 | 關鍵指令 | 章節 | Java 模組 |
|---|---|---|---|---|
| 快取查詢結果 | String / Hash / JSON | `SET EX`、`GET`、`HSET` | [1](#1-快取cache-aside) | M04 |
| Session | Hash + TTL | `HSET`、`EXPIRE` | [2](#2-session-儲存) | M01 |
| 計數（PV、按讚、庫存） | String | `INCR`、`DECRBY` | [3](#3-計數器與庫存) | M01 |
| 不重複計數（UV） | HyperLogLog | `PFADD`、`PFCOUNT` | [3](#3-計數器與庫存) | M03 |
| 排行榜 | Sorted Set | `ZADD`、`ZREVRANGE`、`ZRANK` | [4](#4-排行榜) | M02 |
| 分散式鎖 | String | `SET NX PX` + Lua | [5](#5-分散式鎖) | M14 |
| 限流 | String / Sorted Set / Lua | `INCR`+`EXPIRE`、`ZADD` | [6](#6-限流) | M05, M14 |
| 任務佇列 | List / Stream | `LPUSH`+`BRPOP`、`XADD`+`XREADGROUP` | [7](#7-訊息佇列list-vs-stream) | M07 |
| 即時通知 | Pub/Sub | `PUBLISH`、`SUBSCRIBE` | [8](#8-pubsub-即時通知) | — |
| 附近的店 | Geo | `GEOADD`、`GEOSEARCH` | [9](#9-地理位置) | M03 |
| 簽到 / 活躍 | Bitmap | `SETBIT`、`BITCOUNT` | [10](#10-簽到與活躍度) | M03 |
| 去重 / 可能存在 | Bloom Filter | `BF.ADD`、`BF.EXISTS` | [11](#11-去重布隆過濾器) | M03 |
| 冪等（避免重複下單） | String | `SET NX EX` | [12](#12-冪等性) | M14 |
| 延遲任務 | Sorted Set | `ZADD`（score = 時間）、`ZRANGEBYSCORE` | [13](#13-延遲佇列) | — |
| 全域唯一 ID | String | `INCR` | [14](#14-全域唯一-id) | M14 |
| 最近瀏覽 | List | `LPUSH`+`LTRIM` | [15](#15-最近瀏覽固定長度清單) | M02 |
| 標籤 / 共同好友 | Set | `SADD`、`SINTER` | [16](#16-標籤與共同好友) | M02 |
| 全文搜尋 | Search 模組 | `FT.CREATE`、`FT.SEARCH` | — | M11 |
| 文件 / 向量 | JSON、Vector Set | `JSON.SET`、`VADD` | — | M12 |

---

## 1. 快取（Cache-Aside）

**問題**：資料庫查詢慢，同樣的查詢一直重複。
**做法**：應用先查 Redis，沒有再查資料庫並寫回（Cache-Aside）。

```bash
# 讀：先問 Redis
GET product:sku-123
# (nil) → cache miss → 查資料庫 → 寫回，給 TTL
SET product:sku-123 '{"name":"Redis in Action","price":45}' EX 300
GET product:sku-123          # 之後 5 分鐘都命中

# 更新：先更新資料庫，再刪快取（不是更新快取——避免並發下寫入舊值）
DEL product:sku-123
```

**三個經典問題與對策**（Module 04 有完整實作）：

| 問題 | 情境 | 對策 |
|---|---|---|
| **穿透** | 查一個不存在的 id，每次都打到資料庫 | 快取空值 `SET product:999 "" EX 60`；或用布隆過濾器先擋（[§11](#11-去重布隆過濾器)） |
| **擊穿** | 一個熱 key 過期瞬間，上千請求同時打資料庫 | 互斥鎖：只讓一個請求回源（`SET lock:product:123 1 NX EX 5`），其他等或回舊值；或熱 key 不設 TTL、背景更新 |
| **雪崩** | 大量 key 同時過期 | TTL 加隨機抖動：`EX $((300 + RANDOM % 60))` |

```bash
# 擊穿防護：只有拿到鎖的人回源
SET lock:product:sku-123 1 NX EX 5      # OK → 我去查資料庫；nil → 別人在查，等 50ms 再 GET
```

> Hash 版本：`HSET product:sku-123 name "..." price 45` + `EXPIRE product:sku-123 300`，可以只讀一個欄位 `HGET product:sku-123 price`。
> JSON 版本（Redis 8）：`JSON.SET product:sku-123 $ '{...}'`、`JSON.GET product:sku-123 $.price`。

---

## 2. Session 儲存

**問題**：多台應用伺服器要共享登入狀態。
**做法**：Hash 存 session 欄位，TTL 當作閒置逾時，每次請求續期。

```bash
HSET session:abc123 user_id 1001 role admin login_at 1724800000
EXPIRE session:abc123 1800              # 30 分鐘沒動就登出

HGET session:abc123 user_id             # 每次請求驗證
EXPIRE session:abc123 1800              # 滑動續期
TTL session:abc123

DEL session:abc123                      # 登出
```

**坑**：`HSET` 不會改變既有 TTL，但 Hash 被刪光欄位後 key 會消失；Redis 7.4+ 有 `HEXPIRE` 可以對單一欄位設 TTL。

---

## 3. 計數器與庫存

```bash
# 頁面瀏覽數：原子遞增，不需要先讀
INCR page:home:views
INCRBY page:home:views 10
GET page:home:views

# 每日計數自動歸零：key 帶日期 + TTL
INCR stats:2026-08-28:orders
EXPIRE stats:2026-08-28:orders 172800   # 保留兩天

# 庫存扣減：不能扣成負的 → Lua 原子檢查
SET stock:sku-123 5
EVAL "local s = tonumber(redis.call('GET', KEYS[1])) if s and s >= tonumber(ARGV[1]) then return redis.call('DECRBY', KEYS[1], ARGV[1]) else return -1 end" 1 stock:sku-123 2
# 3 → 剩 3；再扣 4 → -1（不足）

# 不重複訪客（UV）：HyperLogLog，12 KB 就能算上億個，誤差 0.81%
PFADD uv:2026-08-28 user1 user2 user3 user1
PFCOUNT uv:2026-08-28                   # 3
PFMERGE uv:week uv:2026-08-27 uv:2026-08-28   # 合併多天
```

---

## 4. 排行榜

**問題**：即時排名、前 N 名、某人第幾名。
**做法**：Sorted Set，score = 分數。

```bash
ZADD leaderboard 95 alice 87 bob 92 carol
ZINCRBY leaderboard 10 bob              # bob 加 10 分 → 97

ZREVRANGE leaderboard 0 2 WITHSCORES    # 前三名（高到低）
ZREVRANK leaderboard carol              # carol 第幾名（0 起算）→ 2
ZSCORE leaderboard carol                # 92
ZCOUNT leaderboard 90 100               # 90–100 分有幾人
ZREVRANGEBYSCORE leaderboard +inf 90 LIMIT 0 10   # 90 分以上前 10 名

# 只保留前 1000 名，避免無限長大
ZREMRANGEBYRANK leaderboard 0 -1001
```

**坑**：同分時依成員字串排序，要「同分先到先贏」可把 score 設成 `分數 × 10^10 + (MAX_TS - 時間戳)`。
每週榜：key 帶週次 `leaderboard:2026-w35` + TTL。

---

## 5. 分散式鎖

**問題**：多台伺服器只能有一個在做某件事（排程、扣庫存）。
**做法**：`SET key value NX PX ms`——NX 保證只有一人拿到，PX 保證持有者掛了鎖會自動釋放，value 是「我是誰」。

```bash
SET lock:report owner-A NX PX 30000     # OK → 拿到；(nil) → 別人持有
SET lock:report owner-B NX PX 30000     # (nil)

# 釋放：一定要「確認是自己的鎖」再刪，而且要原子（Lua）
EVAL "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end" 1 lock:report owner-B   # 0：不是你的
EVAL "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end" 1 lock:report owner-A   # 1：釋放
```

**為什麼不能直接 `DEL`**：A 拿鎖後處理太久，鎖過期，B 拿到鎖；A 做完 `DEL` 把 B 的鎖刪了。

**續約（watchdog）**：任務時間不確定時，持有者每 10 秒 `PEXPIRE lock:report 30000`（也要先比對 value，用 Lua）。

**Redlock**：跨多個獨立 Redis 取鎖的演算法，社群對其安全性有爭議；大多數場景「單一 Redis + 業務端冪等」就夠。Module 14 的 `DistributedLock` 是完整實作。

---

## 6. 限流

### 固定視窗（最簡單）

```bash
# 每個使用者每分鐘 100 次
INCR ratelimit:user:1001:202608281030   # key 帶到分鐘
EXPIRE ratelimit:user:1001:202608281030 60   # 第一次時設
# 回傳值 > 100 → 拒絕
```

坑：視窗邊界問題——10:30:59 打 100 次、10:31:00 再打 100 次，2 秒內 200 次。

### 滑動視窗（Sorted Set）

```bash
# score = 時間戳（ms），member = 唯一值
ZADD ratelimit:user:1001 1724800000123 req-1
ZREMRANGEBYSCORE ratelimit:user:1001 0 1724799940123     # 移除 60 秒前的
ZCARD ratelimit:user:1001                                 # 還剩幾筆 → 與上限比
EXPIRE ratelimit:user:1001 60
```

四個指令要原子 → 用 `MULTI/EXEC` 或 Lua（Module 14 的 `SlidingWindowRateLimiter`）。

### Token Bucket（Lua，允許突發）

```lua
-- KEYS[1] bucket key；ARGV: capacity, refill_per_sec, now_ms, requested
local cap, rate, now, req = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3]), tonumber(ARGV[4])
local b = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(b[1]) or cap
local ts = tonumber(b[2]) or now
tokens = math.min(cap, tokens + (now - ts) / 1000 * rate)
local allowed = 0
if tokens >= req then tokens = tokens - req; allowed = 1 end
redis.call('HSET', KEYS[1], 'tokens', tokens, 'ts', now)
redis.call('PEXPIRE', KEYS[1], math.ceil(cap / rate * 1000))
return allowed
```

Module 05 的 `TokenBucketRateLimiter` 就是這段。

---

## 7. 訊息佇列：List vs Stream

### List（簡單佇列）

```bash
LPUSH queue:email '{"to":"a@x.com"}'        # 生產者
BRPOP queue:email 5                          # 消費者：阻塞最多 5 秒等訊息
```

問題：消費者拿走後崩潰，訊息就丟了；沒有多消費者群組、沒有重播。`BLMOVE queue:email queue:email:processing RIGHT LEFT 5` 可以做「處理中」清單來補救，但很快就會想要 Stream。

### Stream（Module 07）

```bash
# 生產：* 讓 Redis 產生 ID（毫秒時間戳-序號）；MAXLEN ~ 限制長度
XADD orders MAXLEN ~ 100000 * order_id 1001 amount 45
XADD orders * order_id 1002 amount 99
XLEN orders
XRANGE orders - + COUNT 10                   # 讀範圍

# 消費者群組：多個消費者分攤，且有 ACK
XGROUP CREATE orders billing $ MKSTREAM      # $：只讀新的；0：從頭
XREADGROUP GROUP billing worker-1 COUNT 10 BLOCK 5000 STREAMS orders >     # > = 沒給過別人的
XACK orders billing 1724800000123-0          # 處理完確認

# 故障處理：worker-1 崩潰，它拿走沒 ACK 的訊息在 PEL（pending list）
XPENDING orders billing - + 10               # 看誰欠 ACK、欠多久
XAUTOCLAIM orders billing worker-2 60000 0   # 把閒置 > 60 秒的轉給 worker-2（Redis 6.2+）
XINFO GROUPS orders                          # lag：還沒被讀的數量
```

| | List | Stream |
|---|---|---|
| 訊息保留 | 拿走就沒了 | 保留到被 trim |
| 多消費者 | 競爭同一條 | Consumer Group 分攤 + 各群組獨立 |
| 確認機制 | 無 | XACK + PEL |
| 重播 | 不行 | 任意 ID 開始讀 |
| 適合 | 簡單背景工作 | 事件驅動、需要可靠性 |

Kafka 與 Stream 的差別：Stream 在單一 Redis 的記憶體裡，沒有分區與副本語意（Cluster 下一個 Stream 就是一個 key）；資料量大、要長期保留還是 Kafka。

---

## 8. Pub/Sub 即時通知

```bash
# 終端 1
SUBSCRIBE chat:room1
# 終端 2
PUBLISH chat:room1 "hello"       # 回傳收到的訂閱者數
PSUBSCRIBE chat:*                # 模式訂閱
```

**特性**：fire-and-forget——沒有訂閱者就丟掉、訂閱者斷線期間的訊息收不到、沒有持久化。
適合「即時、丟了沒關係」：聊天室、儀表板推播、快取失效通知（Spring 的 `RedisMessageListenerContainer`）。
需要可靠就用 Stream。Cluster 下用 `SPUBLISH / SSUBSCRIBE`（Sharded Pub/Sub，7.0+）避免全叢集廣播。

---

## 9. 地理位置

```bash
GEOADD stores 121.5654 25.0330 taipei-101 121.5170 25.0478 main-station 121.5598 25.0339 sogo
GEODIST stores taipei-101 main-station km          # 5.15
GEOSEARCH stores FROMLONLAT 121.56 25.03 BYRADIUS 2 km ASC WITHDIST     # 2 公里內的店，近到遠
GEOSEARCH stores FROMMEMBER taipei-101 BYBOX 4 4 km                     # 以某個成員為中心的方框
GEOPOS stores sogo
```

底層是 Sorted Set（score = geohash），所以 `ZREM stores sogo` 可以刪、`ZCARD` 可以數。精度到公尺級，適合「附近的 X」；要精準的多邊形查詢請用 PostGIS。

---

## 10. 簽到與活躍度

**問題**：一億使用者每天是否登入，要算連續簽到、月活。
**做法**：Bitmap——一個使用者一個 bit，一億人一天 12 MB。

```bash
SETBIT signin:2026-08:1001 0 1        # 使用者 1001 在 8/1 簽到（offset = 日 - 1）
SETBIT signin:2026-08:1001 1 1
SETBIT signin:2026-08:1001 27 1
BITCOUNT signin:2026-08:1001          # 本月簽到天數 → 3
GETBIT signin:2026-08:1001 27         # 8/28 有簽到嗎 → 1
BITPOS signin:2026-08:1001 0          # 第一個沒簽到的日子

# 反過來：一天一個 key，bit = 使用者 id → 算 DAU、連續活躍
SETBIT active:2026-08-28 1001 1
BITOP AND active:both active:2026-08-27 active:2026-08-28     # 兩天都活躍的
BITCOUNT active:both
```

---

## 11. 去重：布隆過濾器

**問題**：「這個 URL 爬過了嗎」「這個 email 註冊過嗎」——集合太大放不進 Set。
**做法**：Bloom Filter（Redis 8 內建）：會說「絕對沒有」或「可能有」，1% 誤判率下每個元素約 10 bits。

```bash
BF.RESERVE crawled 0.01 1000000       # 誤判率 1%、預估 100 萬個
BF.ADD crawled "https://a.com/1"
BF.EXISTS crawled "https://a.com/1"   # 1（可能有）
BF.EXISTS crawled "https://a.com/2"   # 0（絕對沒有）
BF.MADD crawled u1 u2 u3
BF.INFO crawled
```

用在快取穿透防護：先問 BF 有沒有這個 id，沒有就直接回 404，不查快取也不查資料庫。
需要刪除元素用 Cuckoo Filter（`CF.ADD / CF.DEL`）。Module 03 的 `BloomFilterService`。

---

## 12. 冪等性

**問題**：使用者連點兩次、網路重送，同一筆訂單被建立兩次。
**做法**：請求帶唯一 id，`SET NX` 只讓第一次通過。

```bash
SET idempotent:order:req-7f3a "processing" NX EX 86400     # OK → 第一次，去處理
SET idempotent:order:req-7f3a "processing" NX EX 86400     # (nil) → 重複，回上次結果
SET idempotent:order:req-7f3a '{"order_id":1001}' XX       # 處理完把結果存回去（XX：只在存在時）
GET idempotent:order:req-7f3a
```

Module 14 的 `IdempotencyService`。

---

## 13. 延遲佇列

**問題**：訂單 30 分鐘未付款自動取消、稍後重試。
**做法**：Sorted Set，score = 到期時間戳，輪詢取出到期的。

```bash
ZADD delayed:cancel-order 1724801800 order:1001       # 30 分鐘後
ZADD delayed:cancel-order 1724801900 order:1002
# 消費者每秒：
ZRANGEBYSCORE delayed:cancel-order 0 1724801800 LIMIT 0 10    # 到期的
ZREM delayed:cancel-order order:1001                            # 取到才算你的（多消費者時用 Lua 把 RANGE+REM 包成原子）
```

Redis 沒有原生延遲佇列；資料量大時 Stream + 應用端排程或專門的佇列系統更合適。

---

## 14. 全域唯一 ID

```bash
INCR id:order                        # 單調遞增、原子；多台應用共用
INCRBY id:order 1000                 # 一次拿一段（號段模式），本地用完再拿，減少往返
```

要「有時間資訊」的 ID：`時間戳 × 10^6 + INCR 值 % 10^6`（Module 14 的 `GlobalIdGenerator`）。要跨機房不依賴 Redis 用 Snowflake / UUID v7。

---

## 15. 最近瀏覽（固定長度清單）

```bash
LPUSH recent:user:1001 product-5
LPUSH recent:user:1001 product-3
LREM recent:user:1001 0 product-5    # 重複瀏覽先移除舊的
LPUSH recent:user:1001 product-5
LTRIM recent:user:1001 0 9           # 只留 10 筆
LRANGE recent:user:1001 0 -1
```

`LPUSH` + `LTRIM` 放進 `MULTI/EXEC`，清單永遠不會超過 10。

---

## 16. 標籤與共同好友

```bash
SADD tags:post:42 redis nosql cache
SADD tags:post:43 redis kafka
SINTER tags:post:42 tags:post:43     # 共同標籤 → redis
SUNION tags:post:42 tags:post:43     # 全部標籤
SDIFF tags:post:42 tags:post:43      # 42 有 43 沒有

SADD friends:alice bob carol dave
SADD friends:bob alice carol eve
SINTER friends:alice friends:bob     # 共同好友 → carol
SINTERCARD 2 friends:alice friends:bob   # 只要數量（7.0+）
SISMEMBER friends:alice bob          # 是不是好友
SRANDMEMBER friends:alice 2          # 隨機推薦
```

大集合的 `SINTER` 是 O(N×M)，百萬級好友請預先計算或限制。

---

## 練習方式

1. `docker compose up -d redis redis-insight`，打開 <http://localhost:5540> 的 Workbench
2. 每個場景的指令貼進去跑一遍，觀察回傳值；改幾個參數看會怎樣
3. 跑 `./scripts/smoke-test.sh` 看自動化斷言（28 項）
4. 再去讀對應 Java 模組的測試（`module-XX/src/test`），看同一個模式在 Spring Data Redis 裡長什麼樣
