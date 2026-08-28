# 05 · 應用場景：用 redis-cli 走一遍 16 個經典模式

> 學 Redis 最常見的卡關不是「指令不會敲」，而是「不知道這個問題該用哪個資料結構、為什麼不是別的」。
> 所以這一章每個場景都固定四段：**遇到什麼問題 → 為什麼選這個結構（而不是別的）→ 指令走一遍（每一步在解決什麼）→ 坑是怎麼發生的、後果是什麼、怎麼避**，最後對應到本專案的 Java 模組。
> 指令全部可以貼進 `redis-cli`（或 RedisInsight 的 Workbench）直接執行；`scripts/smoke-test.sh` 會自動跑其中的關鍵斷言（28 項）。

## 選型速查表

**怎麼用這張表**：先從左邊找你的「需求」，再看第二欄「為什麼是這個結構」——如果那個理由不符合你的狀況（例如你需要的不是「排名」而是「精確計數」），就該換一列。直接看第三欄的指令去套，往往會選到能動但不合適的結構（例如拿 List 當佇列，上線後才發現訊息會丟）。每一列都有對應章節，裡面有完整的推理。

| 需求 | 為什麼是這個結構 | 資料結構 | 關鍵指令 | 章節 | Java 模組 |
|---|---|---|---|---|---|
| 快取查詢結果 | 要的是「整包拿、到期自動消失」，String + TTL 最簡單；要部分欄位才用 Hash / JSON | String / Hash / JSON | `SET EX`、`GET`、`HSET` | [1](#1-快取cache-aside) | M04 |
| Session | 一個 session 有多個欄位、整個一起過期 → Hash + TTL | Hash + TTL | `HSET`、`EXPIRE` | [2](#2-session-儲存) | M01 |
| 計數（PV、按讚、庫存） | 需要原子加減，String 的 `INCR` 不用先讀再寫 | String | `INCR`、`DECRBY` | [3](#3-計數器與庫存) | M01 |
| 不重複計數（UV） | 精確去重要存所有 id 記憶體爆炸，HLL 用 12 KB 換 0.81% 誤差 | HyperLogLog | `PFADD`、`PFCOUNT` | [3](#3-計數器與庫存) | M03 |
| 排行榜 | 要「依分數排序 + 查名次」，只有 Sorted Set 兩者都是 O(log N) | Sorted Set | `ZADD`、`ZREVRANGE`、`ZRANK` | [4](#4-排行榜) | M02 |
| 分散式鎖 | 需要「不存在才建立」的原子語意 + 自動過期 | String | `SET NX PX` + Lua | [5](#5-分散式鎖) | M14 |
| 限流 | 依精度需求：計數（固定視窗）、時間戳集合（滑動）、Lua（token bucket） | String / Sorted Set / Lua | `INCR`+`EXPIRE`、`ZADD` | [6](#6-限流) | M05, M14 |
| 任務佇列 | List 拿走就沒了；需要 ACK / 重播 / 多消費者就要 Stream | List / Stream | `LPUSH`+`BRPOP`、`XADD`+`XREADGROUP` | [7](#7-訊息佇列list-vs-stream) | M07 |
| 即時通知 | 丟了沒關係、不要占記憶體 → Pub/Sub 的 fire-and-forget 剛好 | Pub/Sub | `PUBLISH`、`SUBSCRIBE` | [8](#8-pubsub-即時通知) | — |
| 附近的店 | 經緯度轉 geohash 放進 Sorted Set，範圍查詢變成 score 區間 | Geo | `GEOADD`、`GEOSEARCH` | [9](#9-地理位置) | M03 |
| 簽到 / 活躍 | 每人每天只需 1 bit，一億人一天 12 MB | Bitmap | `SETBIT`、`BITCOUNT` | [10](#10-簽到與活躍度) | M03 |
| 去重 / 可能存在 | 集合太大放不下，接受「可能有」的誤判換 10 bits/元素 | Bloom Filter | `BF.ADD`、`BF.EXISTS` | [11](#11-去重布隆過濾器) | M03 |
| 冪等（避免重複下單） | 「第一次才通過」= `SET NX`；TTL 限制去重窗口 | String | `SET NX EX` | [12](#12-冪等性) | M14 |
| 延遲任務 | score 當「到期時間」，取出到期的就是 score 區間查詢 | Sorted Set | `ZADD`、`ZRANGEBYSCORE` | [13](#13-延遲佇列) | — |
| 全域唯一 ID | 多台機器共用一個原子遞增計數器 | String | `INCR` | [14](#14-全域唯一-id) | M14 |
| 最近瀏覽 | 有序、從頭插入、截斷尾巴，List 三個都是 O(1) | List | `LPUSH`+`LTRIM` | [15](#15-最近瀏覽固定長度清單) | M02 |
| 標籤 / 共同好友 | 要的是集合運算（交集、聯集），Set 伺服器端直接算 | Set | `SADD`、`SINTER` | [16](#16-標籤與共同好友) | M02 |
| 全文搜尋 | 核心結構沒有倒排索引，要 Search 模組 | Search 模組 | `FT.CREATE`、`FT.SEARCH` | — | M11 |
| 文件 / 向量 | 巢狀 JSON 路徑更新、向量相似度，核心結構做不到 | JSON、Vector Set | `JSON.SET`、`VADD` | — | M12 |

---

## 1. 快取（Cache-Aside）

### 問題

資料庫查一次要 10–50 ms，而同一個商品頁每秒被看幾百次——**大部分查詢都在重複算同一個答案**。快取的目的就是把「算過的答案」放在記憶體，讓第二次以後的請求不再碰資料庫。

### 為什麼是 Cache-Aside、為什麼是 String + TTL

- 快取模式有 Cache-Aside、Read-Through、Write-Behind（Module 04 都有實作）。**Cache-Aside 最常用是因為它最簡單、不需要中介層**：應用自己決定什麼時候讀、什麼時候寫，Redis 只是個 key-value。
- 用 String 是因為快取通常是「整包序列化（JSON）」，讀寫都是一次到位；用 TTL 是因為**快取一定會過時**——你沒有辦法在每個資料庫變動時都完美同步，TTL 是最後一道兜底：最多錯 N 秒。
- 只需要一個欄位時才改用 Hash（`HGET product:sku-123 price`）；要對 JSON 內部路徑做原子更新才用 JSON 型別。

### 指令走一遍

第一步：讀取先問 Redis。沒有（cache miss）就查資料庫，然後**把答案寫回 Redis 並給 TTL**——沒給 TTL 的快取只會漲、永遠不會消失。

```bash
GET product:sku-123
# (nil) → cache miss → 查資料庫 → 寫回，給 TTL
SET product:sku-123 '{"name":"Redis in Action","price":45}' EX 300
GET product:sku-123          # 之後 5 分鐘都命中
```

第二步：資料更新時，**先更新資料庫，再刪快取**（不是更新快取）。原因：兩個請求同時更新時，「更新快取」可能讓後寫入的舊值蓋掉新值；「刪快取」則讓下一次讀取重新從資料庫載入最新值，永遠不會留下舊資料。

```bash
DEL product:sku-123
```

### 三個經典問題：怎麼發生、後果、對策

這三個問題都是「快取沒接到，流量直接打到資料庫」，差別在原因。Module 04 有完整實作。

| 問題 | 怎麼發生 | 後果 | 對策 |
|---|---|---|---|
| **穿透** | 有人一直查**根本不存在**的 id（惡意或 bug），快取永遠沒有東西可存 | 每次都打資料庫，等於沒有快取 | 把「不存在」也快取起來 `SET product:999 "" EX 60`；或用布隆過濾器先擋（[§11](#11-去重布隆過濾器)） |
| **擊穿** | 一個**熱門** key 剛好過期，這一瞬間上千個請求同時 miss | 上千個相同查詢同時打資料庫，可能把它壓垮 | 互斥鎖：只讓一個請求回源，其他人等或回舊值；或熱 key 不設 TTL、由背景工作更新 |
| **雪崩** | 大量 key 在**同一秒**過期（例如系統啟動時一起載入、TTL 都是 300） | 那一秒所有請求都 miss，資料庫瞬間承受全量 | TTL 加隨機抖動 `EX $((300 + RANDOM % 60))`，讓過期時間散開 |

擊穿的互斥鎖做法：拿到鎖的人才去查資料庫，其他人短暫等待後再 `GET`（那時通常已經有值了）。

```bash
SET lock:product:sku-123 1 NX EX 5      # OK → 我去查資料庫；nil → 別人在查，等 50ms 再 GET
```

> Hash 版本：`HSET product:sku-123 name "..." price 45` + `EXPIRE product:sku-123 300`，可以只讀一個欄位 `HGET product:sku-123 price`。
> JSON 版本（Redis 8）：`JSON.SET product:sku-123 $ '{...}'`、`JSON.GET product:sku-123 $.price`。

---

## 2. Session 儲存

### 問題

應用伺服器有多台、前面掛負載平衡，使用者這次請求打到 A、下次打到 B——**登入狀態如果只放在 A 的記憶體，B 就不認得他**。Session 要放在所有伺服器都看得到的地方，而且要「一段時間沒動就自動登出」。

### 為什麼是 Hash + TTL

- Session 天生是「一組欄位」（user_id、role、登入時間），用 Hash 可以**只讀需要的欄位**（驗證時只要 user_id），也可以只更新一個欄位；塞成一個 JSON String 則每次都要整包讀寫。
- TTL 掛在整個 key 上，正好對應「整個 session 一起過期」的語意；閒置逾時就是每次請求把 TTL 重設。

### 指令走一遍

建立 session：寫欄位，然後設 TTL——**兩個動作要一起做**，只寫 Hash 不設 TTL 的 session 永遠不會登出、記憶體只增不減。

```bash
HSET session:abc123 user_id 1001 role admin login_at 1724800000
EXPIRE session:abc123 1800              # 30 分鐘沒動就登出
```

每次請求：讀 user_id 驗證身分，並**重設 TTL**（滑動續期）——這就是「30 分鐘沒動才登出」而不是「登入後 30 分鐘一定登出」。

```bash
HGET session:abc123 user_id
EXPIRE session:abc123 1800
TTL session:abc123                      # 看還剩多久，方便除錯
```

登出：直接刪掉。

```bash
DEL session:abc123
```

### 坑

- `HSET` **不會**改變既有 TTL（所以上面要另外 `EXPIRE`），但如果你把 Hash 的欄位全部 `HDEL` 光，整個 key 會消失、TTL 也跟著沒了——之後再 `HSET` 就是一個沒有 TTL 的新 key。
- 想對「單一欄位」設不同 TTL（例如 token 30 分鐘、偏好設定 7 天）：Redis 7.4+ 有 `HEXPIRE`；舊版只能拆成兩個 key。

---

## 3. 計數器與庫存

### 問題

「按讚數 +1」如果寫成「讀出來、加一、寫回去」，兩個請求同時做就會**各自讀到 10、各自寫回 11**，少算一次。計數需要的是**原子**的加減——Redis 單執行緒執行指令，`INCR` 天生原子，不需要鎖。

### 為什麼是 String 的 INCR

String 存的整數用 `int` 編碼，`INCR` 直接在數值上加，一個指令完成「讀 + 加 + 寫」，任何並發都不會互相蓋掉。這是 Redis 最基本也最常用的原子操作。

### 指令走一遍

頁面瀏覽數：不需要先 `GET`，直接遞增。

```bash
INCR page:home:views
INCRBY page:home:views 10
GET page:home:views
```

每日計數自動歸零：與其寫排程去清零，不如**把日期放進 key、加 TTL**——新的一天自然是新的 key，舊的過期自動消失。

```bash
INCR stats:2026-08-28:orders
EXPIRE stats:2026-08-28:orders 172800   # 保留兩天，隔天還能查昨天
```

庫存扣減：`DECRBY` 也是原子的，但它**不會檢查會不會扣成負數**——兩個人同時買最後一件，兩個都會成功、庫存變 -1。所以「檢查 + 扣減」要在一個 Lua 腳本裡完成（Lua 執行期間不會有其他指令插進來）。

```bash
SET stock:sku-123 5
EVAL "local s = tonumber(redis.call('GET', KEYS[1])) if s and s >= tonumber(ARGV[1]) then return redis.call('DECRBY', KEYS[1], ARGV[1]) else return -1 end" 1 stock:sku-123 2
# 3 → 剩 3；再扣 4 → -1（不足）
```

不重複訪客（UV）：要「今天有幾個**不同的**人來過」，用 Set 存所有 user id 在千萬級使用者下要幾百 MB；HyperLogLog 用固定 12 KB 估算基數，誤差 0.81%——UV 報表不需要精確到個位數，這個交換划算。

```bash
PFADD uv:2026-08-28 user1 user2 user3 user1
PFCOUNT uv:2026-08-28                   # 3（user1 重複不算）
PFMERGE uv:week uv:2026-08-27 uv:2026-08-28   # 合併多天算週 UV，不用重新掃資料
```

### 坑

- `INCR` 對非整數的 String 會回 `ERR value is not an integer`；key 不存在則從 0 開始，這通常是你要的。
- HLL 只能問「有幾個」，不能問「某人來過嗎」——需要後者請用 Set 或 Bitmap（[§10](#10-簽到與活躍度)）。

---

## 4. 排行榜

### 問題

遊戲積分榜要做三件事：更新某人分數、取前 N 名、查某人第幾名——而且分數隨時在變。用關聯式資料庫 `ORDER BY score DESC LIMIT 10` 每次都要排序，百萬玩家下每秒幾百次查詢會撐不住。

### 為什麼是 Sorted Set 而不是 Hash 或 List

- Hash 可以存「人 → 分數」，但**沒有順序**，取前 10 名要把全部撈出來自己排。
- List 有順序，但插入新分數要找位置，是 O(N)。
- Sorted Set 內部是跳表 + 雜湊表：**依分數排序、插入 / 更新 / 查名次都是 O(log N)**，取前 N 名是 O(log N + N)。它就是為「排序 + 查名次」設計的結構。

### 指令走一遍

寫入分數：`ZADD` 新增或更新（同一個成員再 `ZADD` 就是改分數）；累加用 `ZINCRBY`，一樣是原子的。

```bash
ZADD leaderboard 95 alice 87 bob 92 carol
ZINCRBY leaderboard 10 bob              # bob 加 10 分 → 97
```

查詢：前 N 名用 `ZREVRANGE`（REV = 分數高的在前），名次用 `ZREVRANK`（0 起算），分數區間用 `ZCOUNT` / `ZREVRANGEBYSCORE`。

```bash
ZREVRANGE leaderboard 0 2 WITHSCORES    # 前三名（高到低）
ZREVRANK leaderboard carol              # carol 第幾名（0 起算）→ 2
ZSCORE leaderboard carol                # 92
ZCOUNT leaderboard 90 100               # 90–100 分有幾人
ZREVRANGEBYSCORE leaderboard +inf 90 LIMIT 0 10   # 90 分以上前 10 名
```

控制大小：榜單如果不裁剪會隨玩家數無限長大，變成大 key（[04 §4](04-performance-tuning.md#4-大-key-與熱-key)）。只有前 1000 名有意義的話，定期把後面的移掉。

```bash
ZREMRANGEBYRANK leaderboard 0 -1001     # 只保留前 1000 名
```

### 坑

- **同分怎麼排**：Sorted Set 同分時依成員字串的字典序排，不是先到先贏。要「同分先到先贏」可把 score 設成 `分數 × 10^10 + (MAX_TS - 時間戳)`——分數為主、時間戳當次要排序鍵。
- **週榜 / 月榜**：不要用同一個 key 然後排程清空（清空瞬間榜是空的）；key 帶週次 `leaderboard:2026-w35` + TTL，新的一週自然開新榜。

---

## 5. 分散式鎖

### 問題

多台應用伺服器都跑同一個排程（例如每天結算），沒有協調的話**每台都會跑一次**；或兩個請求同時扣同一筆庫存。你需要「同一時間只有一個人能做這件事」——單機用 mutex，跨機器就要一個大家都看得到的鎖，Redis 是最常見的選擇。

### 為什麼是 SET NX PX，為什麼 value 要放「我是誰」

一把可用的分散式鎖要同時滿足三件事，`SET key value NX PX ms` 一個指令全包：
- **互斥**：`NX`（不存在才設定）保證只有一個人能建立這個 key。
- **不會死鎖**：持有者如果當機、永遠不釋放，其他人就永遠拿不到。`PX` 給鎖一個到期時間，最壞情況等它自然過期。
- **只能由持有者釋放**：value 放一個隨機值（「我是誰」），釋放時先比對——原因見下面的坑。

早期做法 `SETNX` + `EXPIRE` 是兩個指令，中間當機就會留下沒有 TTL 的鎖；`SET … NX PX` 把兩者合成一個原子操作，這是唯一正確的寫法。

### 指令走一遍

取鎖：成功回 `OK`，被別人持有回 `nil`。

```bash
SET lock:report owner-A NX PX 30000     # OK → 拿到；(nil) → 別人持有
SET lock:report owner-B NX PX 30000     # (nil)
```

釋放：**先確認 value 是自己的再刪，而且這兩步必須原子**——所以用 Lua。

```bash
EVAL "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end" 1 lock:report owner-B   # 0：不是你的
EVAL "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end" 1 lock:report owner-A   # 1：釋放
```

### 坑：為什麼不能直接 DEL

情境：A 拿到鎖（30 秒），但任務跑了 40 秒。第 30 秒鎖過期，B 拿到鎖開始工作。第 40 秒 A 做完了執行 `DEL`——**刪掉的是 B 的鎖**。接著 C 又能拿到鎖，B 和 C 同時在做「只能一個人做」的事。比對 value 就是為了讓 A 發現「這把鎖已經不是我的」。

**續約（watchdog）**：任務時間不確定時，把 TTL 設短一點，持有者每隔一段時間（例如 TTL 的 1/3）`PEXPIRE lock:report 30000` 延長——同樣要先比對 value（用 Lua），不然會幫別人續約。

**Redlock**：在多個獨立 Redis 上同時取鎖、過半成功才算拿到，目的是不依賴單一 Redis。社群對它在時鐘漂移下的安全性有爭議；大多數場景「單一 Redis（或 Sentinel）+ 業務端冪等（[§12](#12-冪等性)）」就夠了，鎖只是減少重複、不是唯一防線。Module 14 的 `DistributedLock` 是完整實作。

---

## 6. 限流

### 問題

一個使用者（或一支爬蟲）每秒打你上千次 API，不擋的話後端會被拖垮、其他人跟著遭殃。限流就是「每個人在一段時間內最多 N 次」。三種做法精度不同、成本不同，先從最簡單的開始。

### 固定視窗（最簡單）

**為什麼**：只需要一個計數器 + 過期時間，一個 `INCR` 就完成，成本最低。把「分鐘」放進 key，每分鐘自動換新視窗。

```bash
# 每個使用者每分鐘 100 次
INCR ratelimit:user:1001:202608281030   # key 帶到分鐘
EXPIRE ratelimit:user:1001:202608281030 60   # 第一次時設，讓舊視窗自動消失
# 回傳值 > 100 → 拒絕
```

**坑：視窗邊界**。10:30:59 打 100 次（視窗 10:30 剛好滿）、10:31:00 再打 100 次（新視窗），**2 秒內 200 次**——限流被繞過一倍。內部工具可以接受，對外 API 通常不行。

### 滑動視窗（Sorted Set）

**為什麼**：要解決邊界問題，就得記住「每一次請求的時間」而不是只記次數，然後每次問「過去 60 秒內有幾次」。Sorted Set 以時間戳為 score，「移除 60 秒前的 + 數剩下的」正好是它擅長的 score 區間操作。

```bash
# score = 時間戳（ms），member = 唯一值（避免同一毫秒的請求互相覆蓋）
ZADD ratelimit:user:1001 1724800000123 req-1
ZREMRANGEBYSCORE ratelimit:user:1001 0 1724799940123     # 移除 60 秒前的
ZCARD ratelimit:user:1001                                 # 還剩幾筆 → 與上限比
EXPIRE ratelimit:user:1001 60                             # 沒人再打時自動清掉
```

**坑**：這四個指令之間如果插進別的請求，計數就不準——要用 `MULTI/EXEC` 或 Lua 包成原子（Module 14 的 `SlidingWindowRateLimiter`）。代價是每次請求存一筆，上限 1000 次/分鐘的話每個使用者最多 1000 個成員。

### Token Bucket（Lua，允許突發）

**為什麼**：前兩種都是「硬上限」，但很多 API 希望允許短暫突發（使用者開頁面時連打 10 次是正常的），只要**平均**不超過。Token bucket：桶子以固定速率補 token、容量有上限，請求拿 token、拿不到就拒絕——平常存滿的 token 就是允許的突發量。它需要「算補了多少 token + 扣 + 寫回」三步原子，所以用 Lua。

```lua
-- KEYS[1] bucket key；ARGV: capacity, refill_per_sec, now_ms, requested
local cap, rate, now, req = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3]), tonumber(ARGV[4])
local b = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(b[1]) or cap                  -- 第一次：桶是滿的
local ts = tonumber(b[2]) or now
tokens = math.min(cap, tokens + (now - ts) / 1000 * rate)   -- 依經過的時間補 token，不超過容量
local allowed = 0
if tokens >= req then tokens = tokens - req; allowed = 1 end
redis.call('HSET', KEYS[1], 'tokens', tokens, 'ts', now)
redis.call('PEXPIRE', KEYS[1], math.ceil(cap / rate * 1000))   -- 桶補滿所需時間後就可以丟掉
return allowed
```

Module 05 的 `TokenBucketRateLimiter` 就是這段。

---

## 7. 訊息佇列：List vs Stream

### 問題

寄 email、產報表這種慢工作不該讓使用者等，要丟到背景「排隊處理」。佇列的難點不在「排」，在**故障**：處理到一半的 worker 當機，那筆工作怎麼辦？這決定了你該用 List 還是 Stream。

### List（簡單佇列）

**為什麼先講 List**：它最簡單——生產者 `LPUSH`、消費者 `BRPOP`（阻塞等，不用輪詢），兩個指令就是一個佇列。適合「丟了也沒關係、重跑就好」的工作。

```bash
LPUSH queue:email '{"to":"a@x.com"}'        # 生產者
BRPOP queue:email 5                          # 消費者：阻塞最多 5 秒等訊息
```

**坑：訊息拿走就沒了**。`BRPOP` 一回傳，那筆訊息就從 List 消失；消費者處理到一半當機，訊息永久遺失。沒有多消費者群組（兩個 worker 只是競爭同一條 List）、沒有重播、不知道「處理了沒」。補救方式是 `BLMOVE queue:email queue:email:processing RIGHT LEFT 5`——先搬到「處理中」清單，做完再刪；但你很快會發現自己在重新發明 Stream。

### Stream（Module 07）

**為什麼要 Stream**：它把「訊息保留、多消費者分工、確認（ACK）、失敗轉交」全部做進資料結構裡，你不用自己維護處理中清單。

生產：`*` 讓 Redis 產生遞增 ID（毫秒時間戳-序號），這個 ID 就是後面 ACK 與重播的依據。**`MAXLEN ~` 一定要加**——Stream 會保留訊息，沒有上限就是一個只漲不減的大 key。

```bash
XADD orders MAXLEN ~ 100000 * order_id 1001 amount 45
XADD orders * order_id 1002 amount 99
XLEN orders
XRANGE orders - + COUNT 10                   # 讀範圍：訊息還在，隨時可以回頭看
```

消費者群組：多個 worker 分攤同一個 Stream，每筆訊息只會給群組內的一個人；`>` 表示「還沒給過任何人的」。處理完 `XACK`，Redis 才會把它從「待確認」清單移除。

```bash
XGROUP CREATE orders billing $ MKSTREAM      # $：從現在開始；0：從頭讀
XREADGROUP GROUP billing worker-1 COUNT 10 BLOCK 5000 STREAMS orders >
XACK orders billing 1724800000123-0
```

故障處理：worker-1 拿走訊息後當機、沒 ACK，那筆訊息會留在 PEL（pending entries list）——這就是 List 做不到的事。用 `XPENDING` 看誰欠 ACK、欠多久，`XAUTOCLAIM` 把閒置太久的轉給別的 worker。

```bash
XPENDING orders billing - + 10               # 看誰欠 ACK、欠多久
XAUTOCLAIM orders billing worker-2 60000 0   # 把閒置 > 60 秒的轉給 worker-2（Redis 6.2+）
XINFO GROUPS orders                          # lag：還沒被讀的數量，監控消費速度用
```

### 怎麼選

| | List | Stream |
|---|---|---|
| 訊息保留 | 拿走就沒了 | 保留到被 trim |
| 多消費者 | 競爭同一條，無法分工 | Consumer Group 分攤；多個群組各自獨立讀同一份 |
| 確認機制 | 無 | XACK + PEL |
| 重播 | 不行 | 任意 ID 開始讀 |
| 適合 | 丟了可重跑的背景工作 | 事件驅動、需要可靠性 |

**Kafka 與 Stream 的差別**：Stream 在單一 Redis 的記憶體裡，Cluster 下一個 Stream 就是一個 key（不會分片），也沒有 Kafka 的分區與副本語意；資料量大、要保留幾天以上還是 Kafka。

---

## 8. Pub/Sub 即時通知

### 問題

聊天室、儀表板即時更新、「有人改了設定通知所有伺服器清快取」——這類訊息的特性是**時效性強、過了就沒意義**：斷線 5 分鐘的使用者不需要補收 5 分鐘前的聊天訊息。

### 為什麼是 Pub/Sub 而不是 Stream

Stream 會保留訊息、占記憶體、需要消費者 ACK；對「過了就沒意義」的訊息這些都是負擔。Pub/Sub 是 fire-and-forget：發佈時有誰在訂閱就送給誰，沒人訂閱就直接丟掉，不占任何記憶體。

```bash
# 終端 1
SUBSCRIBE chat:room1
# 終端 2
PUBLISH chat:room1 "hello"       # 回傳收到的訂閱者數（0 = 沒人在聽，訊息已丟棄）
PSUBSCRIBE chat:*                # 模式訂閱：一次聽所有房間
```

### 坑

- **不可靠是設計，不是 bug**：訂閱者斷線期間的訊息收不到、沒有持久化、Redis 重啟訂閱關係消失。需要可靠就用 Stream（[§7](#7-訊息佇列list-vs-stream)）。
- **Cluster 下的廣播成本**：一般 `PUBLISH` 會把訊息廣播到叢集所有節點（因為訂閱者可能連在任何節點），節點多時很浪費；7.0+ 用 `SPUBLISH / SSUBSCRIBE`（Sharded Pub/Sub）只送到該 channel 所屬的分片。
- **慢訂閱者**：訂閱者收得慢，Redis 端的輸出緩衝會堆積，超過 `client-output-buffer-limit pubsub` 就被踢掉——這是保護 Redis 記憶體的機制，不要把它調到無限大。

Spring 的 `RedisMessageListenerContainer` 就是包裝 Pub/Sub 做快取失效通知的典型用法。

---

## 9. 地理位置

### 問題

「離我 2 公里內的門市」如果用經緯度直接算距離，要對每一家店算一次三角函數，一萬家店每次查詢就是一萬次運算。

### 為什麼是 Geo

Geo 把經緯度編成 geohash（一個 52-bit 整數）當作 Sorted Set 的 score；**地理上相近的點 geohash 也相近**，所以「範圍內的店」變成 score 區間查詢，只需要看少數候選再精算距離。它底層真的就是 Sorted Set，所以 `ZREM`、`ZCARD` 都能用。

```bash
GEOADD stores 121.5654 25.0330 taipei-101 121.5170 25.0478 main-station 121.5598 25.0339 sogo
GEODIST stores taipei-101 main-station km          # 5.15
GEOSEARCH stores FROMLONLAT 121.56 25.03 BYRADIUS 2 km ASC WITHDIST     # 2 公里內的店，近到遠
GEOSEARCH stores FROMMEMBER taipei-101 BYBOX 4 4 km                     # 以某個成員為中心的方框
GEOPOS stores sogo
ZREM stores sogo                                    # 刪除：就是 Sorted Set
```

### 坑

- 參數順序是 **經度、緯度**（longitude, latitude），跟 Google Maps 顯示的「緯度, 經度」相反——寫反了不會報錯，只是所有店都跑到海裡。
- 精度到公尺級、只能做圓形 / 方框查詢；要多邊形（「這個行政區內」）或精確的地理運算請用 PostGIS，Redis 負責「附近的 X」這種熱查詢就好。

---

## 10. 簽到與活躍度

### 問題

一億使用者，每天記錄「今天有沒有登入」，要算連續簽到天數、月活躍數（MAU）。如果用 Set 存每天登入的 user id，一天就是幾百 MB，一個月幾十 GB。

### 為什麼是 Bitmap

每人每天只需要 **1 個 bit**（0 沒來、1 來了）：一億使用者一天 12.5 MB，一個月 375 MB。而且 `BITCOUNT`、`BITOP` 是 CPU 級的位元運算，算 MAU 是毫秒級。Bitmap 其實就是 String，`SETBIT` 把第 N 個 bit 設成 1，N 可以是日期，也可以是 user id——兩種擺法各解決一種問題。

第一種擺法：**一個使用者一個 key，bit = 日期**，回答「這個人這個月簽了幾天」。

```bash
SETBIT signin:2026-08:1001 0 1        # 使用者 1001 在 8/1 簽到（offset = 日 - 1）
SETBIT signin:2026-08:1001 1 1
SETBIT signin:2026-08:1001 27 1
BITCOUNT signin:2026-08:1001          # 本月簽到天數 → 3
GETBIT signin:2026-08:1001 27         # 8/28 有簽到嗎 → 1
BITPOS signin:2026-08:1001 0          # 第一個沒簽到的日子（連續簽到斷在哪）
```

第二種擺法：**一天一個 key，bit = user id**，回答「今天有幾人活躍」「連續兩天都活躍的有幾人」——`BITOP AND` 把兩天的 bitmap 做位元 AND，結果的 1 就是兩天都有的人。

```bash
SETBIT active:2026-08-28 1001 1
BITOP AND active:both active:2026-08-27 active:2026-08-28     # 兩天都活躍的
BITCOUNT active:both
```

### 坑

- bit offset 直接決定 String 長度：`SETBIT key 100000000 1` 會立刻配置 12.5 MB。user id 如果是稀疏的大數字（例如雪花 ID），Bitmap 會浪費大量空間——要先映射成連續的小整數。
- Bitmap 只能回答「是 / 否」；要「幾次」請用 [§3](#3-計數器與庫存) 的計數器。

---

## 11. 去重：布隆過濾器

### 問題

爬蟲要判斷「這個 URL 爬過了嗎」、註冊要判斷「這個 email 用過了嗎」——集合有幾億個元素，用 Set 存不下，查資料庫又太慢。

### 為什麼是 Bloom Filter

它是一種**用誤判換空間**的結構：只回答「絕對沒有」或「可能有」，1% 誤判率下每個元素約 10 bits（Set 存一個 URL 至少 50–100 bytes）。因為「絕對沒有」是可靠的，它適合當**前置過濾器**：BF 說沒有就直接回，說可能有才去查真正的儲存。Redis 8 內建（`BF.*`）。

建立時要先宣告誤判率與預估數量——容量決定 bit 陣列大小，塞超過預估數量誤判率會上升。

```bash
BF.RESERVE crawled 0.01 1000000       # 誤判率 1%、預估 100 萬個
BF.ADD crawled "https://a.com/1"
BF.EXISTS crawled "https://a.com/1"   # 1（可能有）
BF.EXISTS crawled "https://a.com/2"   # 0（絕對沒有）
BF.MADD crawled u1 u2 u3
BF.INFO crawled                       # 看目前元素數、容量
```

### 用途與坑

- **快取穿透防護**（[§1](#1-快取cache-aside)）：把所有存在的商品 id 先加進 BF，查詢時 BF 說沒有就直接回 404，不查快取也不查資料庫。
- **不能刪除元素**：多個元素共用 bit，刪一個會影響其他人。需要刪除用 Cuckoo Filter（`CF.ADD / CF.DEL`），代價是稍多的空間。
- 誤判率是「說可能有但其實沒有」的機率；反過來「說沒有但其實有」永遠不會發生——設計時要依賴的是後者。

Module 03 的 `BloomFilterService`。

---

## 12. 冪等性

### 問題

使用者按了「下單」沒反應又按一次、手機網路重送請求、訊息佇列 at-least-once 重複投遞——同一筆訂單被建立兩次、扣款兩次。**重試是分散式系統的常態**，所以「同一個請求做兩次，結果跟做一次一樣」（冪等）必須由伺服器保證。

### 為什麼是 SET NX EX

做法：每個請求帶一個唯一 id（前端產生的 UUID、或訊息 id），伺服器**用 `SET NX` 搶這個 id**——第一次成功才處理，之後的重複請求搶不到就直接回上次的結果。`NX` 提供「第一次才通過」的原子語意（跟分散式鎖同一個原理），`EX` 限制去重窗口——不可能永遠記住所有請求 id，通常保留 24 小時（超過這個時間的重試已經不合理）。

```bash
SET idempotent:order:req-7f3a "processing" NX EX 86400     # OK → 第一次，去處理
SET idempotent:order:req-7f3a "processing" NX EX 86400     # (nil) → 重複，回上次結果
SET idempotent:order:req-7f3a '{"order_id":1001}' XX       # 處理完把結果存回去（XX：只在存在時），重複請求可以拿到一樣的回應
GET idempotent:order:req-7f3a
```

### 坑

- 第一次請求「搶到 id 但處理到一半當機」：key 停在 `processing`，重試會一直被當成重複。要嘛處理失敗時 `DEL` 這個 key，要嘛把 `processing` 給一個短 TTL、成功後再用 `XX` 改成結果並延長。
- 冪等 key 要包含「誰 + 做什麼」（`order:req-7f3a`），不同動作不要共用。

Module 14 的 `IdempotencyService`。

---

## 13. 延遲佇列

### 問題

「訂單 30 分鐘未付款自動取消」「失敗的任務 5 分鐘後重試」——工作不是現在做，是**指定時間到了才做**。List / Stream 是先進先出，沒有「到期」的概念。

### 為什麼是 Sorted Set

把 score 設成「應該執行的時間戳」，Sorted Set 就自動依執行時間排序；消費者定期問「score ≤ 現在的有哪些」（`ZRANGEBYSCORE`），這就是延遲佇列。取出後 `ZREM` 表示認領。

```bash
ZADD delayed:cancel-order 1724801800 order:1001       # 30 分鐘後
ZADD delayed:cancel-order 1724801900 order:1002
# 消費者每秒：
ZRANGEBYSCORE delayed:cancel-order 0 1724801800 LIMIT 0 10    # 到期的
ZREM delayed:cancel-order order:1001                            # 取到才算你的
```

### 坑

- **多個消費者會搶到同一筆**：A 和 B 同時 `ZRANGEBYSCORE` 看到 order:1001，都去處理。`ZREM` 的回傳值（1 = 我刪成功、0 = 別人先刪了）可以當作認領依據，或把 RANGE + REM 包進 Lua 讓「取出並移除」原子。
- 這是輪詢，精度取決於輪詢間隔；到期任務很多時單一 Sorted Set 會變大 key。資料量大、要嚴格保證的場景用 Stream + 應用端排程，或專門的佇列系統。

---

## 14. 全域唯一 ID

### 問題

訂單號要在多台應用伺服器之間**不重複且遞增**。資料庫自增欄位在分庫分表後會撞號；UUID 不重複但沒有順序、太長、對索引不友善。

### 為什麼是 INCR

Redis 單執行緒 + `INCR` 原子，多台機器同時要號也不會拿到相同值，天然就是一個全域計數器。頻繁要號時每次一個網路往返太貴，改用**號段模式**：`INCRBY 1000` 一次拿 1000 個號在本地慢慢用。

```bash
INCR id:order                        # 單調遞增、原子；多台應用共用
INCRBY id:order 1000                 # 一次拿一段（號段模式），本地用完再拿，減少往返
```

### 坑

- Redis 是單點：它掛了就發不出號；持久化最多丟 1 秒的話，重啟後可能**重發**已經用過的號——所以號段模式要在拿到號段時就「跳過」一段（例如重啟後先 `INCRBY 10000`）。
- 要 ID 帶時間資訊（方便依時間排序 / 分片）：`時間戳 × 10^6 + INCR 值 % 10^6`（Module 14 的 `GlobalIdGenerator`）；要跨機房、不想依賴 Redis 用 Snowflake / UUID v7。

---

## 15. 最近瀏覽（固定長度清單）

### 問題

「你最近看過的 10 件商品」：新的插在最前面、只留 10 筆、同一件商品再看要移到最前面。

### 為什麼是 List

List 從頭插入（`LPUSH`）、裁掉尾巴（`LTRIM`）都是 O(1)，天生就是「固定長度、最新在前」的結構；`LREM` 負責去重。Sorted Set 也能做（score = 時間戳），但這裡不需要查名次，List 更省。

```bash
LPUSH recent:user:1001 product-5
LPUSH recent:user:1001 product-3
LREM recent:user:1001 0 product-5    # 重複瀏覽先移除舊的（不然清單裡會有兩個 product-5）
LPUSH recent:user:1001 product-5     # 再插到最前面
LTRIM recent:user:1001 0 9           # 只留 10 筆：不裁的話這個 List 會跟著瀏覽紀錄無限長大
LRANGE recent:user:1001 0 -1
```

### 坑

`LPUSH` 和 `LTRIM` 之間如果插進另一個 `LPUSH`，清單會短暫超過 10——把它們放進 `MULTI/EXEC`，清單永遠不會超過上限。`LREM` 是 O(N)，但 N 最多 10，可以接受。

---

## 16. 標籤與共同好友

### 問題

「兩篇文章有哪些共同標籤」「我和他的共同好友」「可能認識的人」——這些都是**集合運算**（交集、聯集、差集）。用關聯式資料庫要 JOIN 自己；在應用端算則要把兩個集合都撈回來。

### 為什麼是 Set

Set 保證成員不重複（`SADD` 同一個成員第二次回 0），而且 `SINTER / SUNION / SDIFF` 在伺服器端直接算，一次往返拿到結果。

標籤：

```bash
SADD tags:post:42 redis nosql cache
SADD tags:post:43 redis kafka
SINTER tags:post:42 tags:post:43     # 共同標籤 → redis
SUNION tags:post:42 tags:post:43     # 全部標籤
SDIFF tags:post:42 tags:post:43      # 42 有 43 沒有
```

好友：

```bash
SADD friends:alice bob carol dave
SADD friends:bob alice carol eve
SINTER friends:alice friends:bob     # 共同好友 → carol
SINTERCARD 2 friends:alice friends:bob   # 只要數量（7.0+）：不用把整個交集傳回來
SISMEMBER friends:alice bob          # 是不是好友：O(1)
SRANDMEMBER friends:alice 2          # 隨機推薦
```

### 坑

`SINTER` 是 O(N×M)（最小集合大小 × 集合數）：兩個各百萬成員的好友列表做交集會卡住主執行緒（[04 §3](04-performance-tuning.md#3-慢指令on-與阻塞)）。名人帳號的共同好友請預先計算、快取結果，或限制只算前 N 個。

---

## 練習方式

為什麼建議這個順序：先在 CLI 敲過、看到回傳值，你才會知道每個指令「做了什麼」；再看自動化斷言，確認自己的理解沒有錯；最後讀 Java 測試，看同一個模式在 Spring Data Redis 裡怎麼包裝。跳過前兩步直接讀 Java，看到的只是 API 呼叫。

1. `docker compose up -d redis redis-insight`，打開 <http://localhost:5540> 的 Workbench
2. 每個場景的指令貼進去跑一遍，觀察回傳值；改幾個參數（TTL、上限、分數）看會怎樣
3. 跑 `./scripts/smoke-test.sh` 看自動化斷言（28 項）
4. 再去讀對應 Java 模組的測試（`module-XX/src/test`），看同一個模式在 Spring Data Redis 裡長什麼樣
