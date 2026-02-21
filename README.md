# Redis 實戰教學專案

> 從零到一，14 個模組帶你掌握 Redis 全棧技能 — 涵蓋資料結構、快取模式、串流處理、搜尋引擎、高可用架構到認證考試準備。

## 技術棧

| 元件 | 版本 | 說明 |
|------|------|------|
| Java | 25 (LTS) | 最新長期支援版 |
| Spring Boot | 4.0.2 | Spring Data Redis + Web + Actuator |
| Redis | 8 | 內建 Search、JSON、TimeSeries、Bloom 模組 |
| Gradle | 8.x | Kotlin DSL + Version Catalog |
| Testcontainers | 2.0.3 | 整合測試自動啟動 Redis 容器 |
| Docker Compose | v2 | Redis + RedisInsight + Prometheus + Grafana |

## 先決條件

開始之前請確認已安裝：

- **JDK 25** — 推薦使用 [SDKMAN](https://sdkman.io/) 安裝：`sdk install java 25-open`
- **Docker Desktop** — 整合測試需要 Docker 執行 Testcontainers
- **Git** — 版本管理

```bash
# 確認版本
java -version    # 需要 25+
docker --version # 需要 Docker Engine 24+
```

## 快速開始

```bash
# 1. 克隆專案
git clone https://github.com/ChunPingWang/redis-tutorial.git
cd redis-tutorial

# 2. 編譯全部模組
./gradlew build

# 3. 執行全部測試（444 個測試）
./gradlew test

# 4. 只執行特定模組的測試
./gradlew :module-01-getting-started:test

# 5. 啟動 Docker 環境（可選，用於手動操作練習）
docker compose up -d
```

## 專案結構

```
redis-tutorial/
├── build.gradle.kts              # 根建構設定（Java 25、共用依賴）
├── settings.gradle.kts           # 多模組註冊
├── gradle/libs.versions.toml     # 集中版本管理
├── docker-compose.yml            # Redis 8 + RedisInsight + Prometheus + Grafana
├── docker-compose-sentinel.yml   # Sentinel 高可用架構（1 Master + 2 Replica + 3 Sentinel）
├── docker-compose-cluster.yml    # Cluster 叢集架構（6 節點）
├── docker/                       # Docker 配置檔
│   ├── redis/redis.conf
│   ├── sentinel/
│   ├── cluster/
│   ├── prometheus/
│   └── grafana/
├── common/                       # 共用模組（RedisConfig、測試基底類別、Quiz 框架）
├── module-01-getting-started/    # 模組 01 ~ 14
├── module-02-data-structures/
├── ...
└── module-14-capstone/
```

### 六角形架構（Hexagonal Architecture）

每個模組都遵循六角形架構，將業務邏輯與框架解耦：

```
com.tutorial.redis.moduleXX/
├── domain/                      # 核心領域 — 零框架依賴
│   ├── model/                   #   Entity、Value Object
│   ├── port/
│   │   ├── inbound/             #   Use Case 介面（驅動端）
│   │   └── outbound/            #   Repository 介面（被驅動端）
│   └── service/                 #   Domain Service（純業務邏輯）
├── application/
│   ├── usecase/                 # Use Case 實作（@Service）
│   └── dto/                     # Command / Query DTO
├── adapter/
│   ├── inbound/rest/            # REST Controller（@RestController）
│   └── outbound/redis/          # Redis Adapter（@Component）
└── infrastructure/config/       # Spring 配置（@Configuration）
```

**為什麼採用六角形架構？**
- `domain/` 層完全不依賴 Spring、Redis — 可獨立單元測試
- `adapter/` 層負責技術細節 — 更換資料庫只需換 Adapter
- `port/` 介面定義契約 — 清晰的依賴方向

## 模組學習路線

### 基礎篇（Module 01-03）

| 模組 | 主題 | Redis 技術 | 測試數 |
|------|------|-----------|--------|
| [Module 01](#module-01-getting-started) | Redis 入門 | String（SET/GET/INCR/TTL） | 15 |
| [Module 02](#module-02-data-structures) | 核心資料結構 | List / Set / Hash / Sorted Set | 18 |
| [Module 03](#module-03-specialized-structures) | 特殊資料結構 | HyperLogLog / Geo / Bitmap / Bloom Filter / TimeSeries | 40 |

### 進階篇（Module 04-06）

| 模組 | 主題 | Redis 技術 | 測試數 |
|------|------|-----------|--------|
| [Module 04](#module-04-caching-patterns) | 快取模式 | Cache-Aside / Read-Through / TTL 策略 | 15 |
| [Module 05](#module-05-pipelining-transactions) | Pipeline 與交易 | Pipeline / MULTI-EXEC / Lua Script | 17 |
| [Module 06](#module-06-data-modeling) | 資料建模 | 嵌入式 vs 引用式 / 反正規化 / Key 設計 | 15 |

### 應用篇（Module 07-10）

| 模組 | 主題 | Redis 技術 | 測試數 |
|------|------|-----------|--------|
| [Module 07](#module-07-streams-events) | 串流與事件驅動 | Streams / Consumer Group / XADD / XREADGROUP | 23 |
| [Module 08](#module-08-persistence) | 持久化 | RDB / AOF / 混合持久化 / RPO-RTO 分析 | 19 |
| [Module 09](#module-09-high-availability) | 高可用（Sentinel） | Sentinel / 自動故障轉移 / 讀寫分離 | 20 |
| [Module 10](#module-10-clustering) | 叢集 | Cluster / Hash Slot（CRC16）/ 拓撲規劃 | 21 |

### 專家篇（Module 11-13）

| 模組 | 主題 | Redis 技術 | 測試數 |
|------|------|-----------|--------|
| [Module 11](#module-11-search-indexing) | 全文搜尋與索引 | RediSearch（FT.CREATE / FT.SEARCH / FT.AGGREGATE） | 15 |
| [Module 12](#module-12-json-vector-search) | JSON 與向量搜尋 | RedisJSON（JSON.SET/GET）/ Vector Search（KNN） | 15 |
| [Module 13](#module-13-security-production) | 安全與正式環境 | ACL / SLOWLOG / INFO / 淘汰策略 / 上線檢查清單 | 10 |

### 總整合（Module 14）

| 模組 | 主題 | Redis 技術 | 測試數 |
|------|------|-----------|--------|
| [Module 14](#module-14-capstone) | Capstone 總整合 | **整合以上全部** + 分散式鎖 / 冪等性 / 限流 | 57 |

> **全專案共 444 個測試，涵蓋單元測試、整合測試與 Quiz 測驗。**

---

## 各模組詳細說明

### Module 01: Getting Started

**學習目標**：認識 Redis 基礎操作，使用 String 型別實作帳戶餘額與商品快取。

```
核心概念：SET / GET / INCR / DECR / TTL / EXPIRE / SETNX
```

**重點程式碼**：
- `RedisAccountCacheAdapter` — 使用 `StringRedisTemplate.opsForValue()` 存取帳戶餘額
- `RedisProductCacheAdapter` — 商品快取搭配 TTL 過期策略

**執行測試**：
```bash
./gradlew :module-01-getting-started:test
```

---

### Module 02: Data Structures

**學習目標**：掌握 Redis 四大核心資料結構的操作與應用場景。

```
List  — LPUSH / RPUSH / LRANGE / BLPOP（訊息佇列、最近瀏覽記錄）
Set   — SADD / SMEMBERS / SINTER / SUNION（標籤系統、共同好友）
Hash  — HSET / HGET / HINCRBY / HGETALL（物件存取、購物車）
ZSet  — ZADD / ZRANGEBYSCORE / ZRANK / ZINCRBY（排行榜、權重排序）
```

**執行測試**：
```bash
./gradlew :module-02-data-structures:test
```

---

### Module 03: Specialized Structures

**學習目標**：使用 Redis 特殊資料結構解決特定領域問題。

```
HyperLogLog  — PFADD / PFCOUNT / PFMERGE（不重複訪客計數）
Geo          — GEOADD / GEODIST / GEOSEARCH（附近門市搜尋）
Bitmap       — SETBIT / GETBIT / BITCOUNT（使用者活躍追蹤）
Bloom Filter — BF.ADD / BF.EXISTS（重複檢測，透過 Lua 腳本）
TimeSeries   — TS.CREATE / TS.ADD / TS.RANGE（時序資料，透過 Lua 腳本）
```

> **注意**：Redis 模組指令（BF.\*、TS.\*）在本專案中一律使用 Lua 腳本搭配 `DefaultRedisScript` 執行，因為 Spring Data Redis 4.x + Lettuce 不直接支援這些指令。

**執行測試**：
```bash
./gradlew :module-03-specialized-structures:test
```

---

### Module 04: Caching Patterns

**學習目標**：學習業界常見的快取策略，處理快取穿透、擊穿、雪崩等問題。

```
Cache-Aside   — 應用層負責讀寫快取
Read-Through  — 快取層自動載入資料
Write-Behind  — 非同步寫回資料庫
TTL 策略      — 固定 / 隨機偏移 / 滑動視窗
```

**執行測試**：
```bash
./gradlew :module-04-caching-patterns:test
```

---

### Module 05: Pipelining & Transactions

**學習目標**：使用 Pipeline 批量操作提升效能，使用 MULTI/EXEC 保證原子性，使用 Lua 腳本實現複雜原子操作。

```
Pipeline   — 批量發送指令減少網路往返
MULTI/EXEC — 交易保證多個指令的原子執行
Lua Script — 伺服器端腳本實現複雜邏輯（Token Bucket 限流）
```

**執行測試**：
```bash
./gradlew :module-05-pipelining-transactions:test
```

---

### Module 06: Data Modeling

**學習目標**：學習 Redis 資料建模的最佳實踐。

```
嵌入式 vs 引用式  — 何時把資料嵌入同一個 Key，何時分散到不同 Key
反正規化          — 為了讀取效能而冗餘存放資料
Key 命名規範      — service:entity:id:field 格式（參見 RedisKeyConvention）
二級索引          — 使用 Set / Sorted Set 建立索引
```

**執行測試**：
```bash
./gradlew :module-06-data-modeling:test
```

---

### Module 07: Streams & Event-Driven

**學習目標**：使用 Redis Streams 實作事件驅動架構，理解 Consumer Group 的運作機制。

```
XADD         — 發布事件到 Stream
XREAD        — 即時讀取事件
XREADGROUP   — Consumer Group 協作消費
XACK         — 確認訊息已處理
Consumer Group — 多消費者並行處理 + 訊息重試
```

**重點**：Consumer Group 建立時需處理 `BUSYGROUP` 例外（已存在的群組），本專案使用 cause-chain 遍歷法正確捕獲。

**執行測試**：
```bash
./gradlew :module-07-streams-events:test
```

---

### Module 08: Persistence

**學習目標**：深入理解 Redis 持久化機制，學會分析 RPO/RTO。

```
RDB（快照）     — save 900 1 / save 300 10 / save 60 10000
AOF（追加日誌） — appendfsync always / everysec / no
混合持久化      — aof-use-rdb-preamble yes（Redis 4.0+）
RPO/RTO 分析   — 根據持久化配置計算資料遺失風險
```

**執行測試**：
```bash
./gradlew :module-08-persistence:test
```

---

### Module 09: High Availability (Sentinel)

**學習目標**：使用 Sentinel 實現 Redis 高可用架構，理解自動故障轉移流程。

```
Sentinel 架構  — 監控 Master + Replica，自動故障轉移
故障轉移流程   — SDOWN → ODOWN → FAILOVER_START → NEW_MASTER → FAILOVER_END
讀寫分離策略   — Master 寫入、Replica 讀取
```

**Docker 環境**：
```bash
docker compose -f docker-compose-sentinel.yml up -d
# 1 Master + 2 Replica + 3 Sentinel
```

**執行測試**：
```bash
./gradlew :module-09-high-availability:test
```

---

### Module 10: Clustering

**學習目標**：理解 Redis Cluster 分片機制，學會 Hash Slot 計算與叢集規劃。

```
Hash Slot     — 16384 個槽位，CRC16(key) % 16384
Hash Tag      — {tag} 語法讓相關 Key 落在同一槽位
MOVED / ASK   — 叢集重新導向機制
叢集拓撲規劃   — 3 Master + 3 Replica 最小生產配置
```

**Docker 環境**：
```bash
docker compose -f docker-compose-cluster.yml up -d
# 6 節點 Cluster（自動初始化）
```

**執行測試**：
```bash
./gradlew :module-10-clustering:test
```

---

### Module 11: Search & Indexing

**學習目標**：使用 RediSearch 建立全文搜尋引擎，支援索引、查詢、聚合與自動完成。

```
FT.CREATE    — 建立搜尋索引（TEXT / TAG / NUMERIC / SORTABLE）
FT.SEARCH    — 全文搜尋查詢
FT.AGGREGATE — 聚合分析（GROUP BY / REDUCE）
FT.SUGADD    — 自動完成建議詞新增
FT.SUGGET    — 自動完成建議詞查詢（支援模糊搜尋）
```

> 所有 FT.\* 指令透過 Lua 腳本 + `cjson.encode()` 執行，回傳 JSON 格式在 Java 端解析。

**執行測試**：
```bash
./gradlew :module-11-search-indexing:test
```

---

### Module 12: JSON & Vector Search

**學習目標**：使用 RedisJSON 存取結構化文件，使用向量搜尋實現語意查詢。

```
JSON.SET     — 儲存 JSON 文件（支援巢狀路徑）
JSON.GET     — 查詢 JSON 文件（JSONPath 語法）
JSON.NUMINCRBY — 原子遞增數值欄位
JSON.ARRAPPEND — 陣列追加元素
Vector Search — KNN 餘弦相似度搜尋（教學用 Java 端實作）
```

**執行測試**：
```bash
./gradlew :module-12-json-vector:test
```

---

### Module 13: Security & Production

**學習目標**：Redis 安全配置、監控指標與上線前檢查。

```
ACL          — 存取控制列表（用戶權限管理）
SLOWLOG      — 慢查詢日誌分析
INFO         — 伺服器狀態監控（memory / stats / clients）
淘汰策略      — 8 種 maxmemory-policy（noeviction / allkeys-lru / volatile-ttl ...）
上線檢查清單  — 16+ 項涵蓋安全、持久化、記憶體、監控等 7 大類別
```

**執行測試**：
```bash
./gradlew :module-13-security-production:test
```

---

### Module 14: Capstone

**學習目標**：整合 Module 01-13 的所有知識，建構兩個完整的子系統。

#### 金融子系統

| 功能 | Redis 技術 | 對應模組 |
|------|-----------|---------|
| 帳戶餘額快取 | String（SET/GET） | M01 |
| 帳戶 Profile | RedisJSON（JSON.SET/GET） | M12 |
| 交易排行榜 | Sorted Set（ZADD/ZREVRANGE） | M02 |
| 詐欺偵測 | Bloom Filter（BF.ADD/EXISTS） | M03 |
| 風險警報串流 | Streams + Consumer Group | M07 |
| 交易全文搜尋 | RediSearch（FT.SEARCH） | M11 |
| 匯率時間序列 | TimeSeries（TS.ADD/GET） | M03 |

#### 電商子系統

| 功能 | Redis 技術 | 對應模組 |
|------|-----------|---------|
| 購物車 | Hash（HSET/HGETALL） | M02 |
| 訂單事件處理 | Streams | M07 |
| 商品快取 | Cache-Aside + TTL | M04 |
| 商品搜尋 + 自動完成 | RediSearch（FT.SEARCH/SUGGET） | M11 |
| 門市定位 | Geo（GEOADD/GEOSEARCH） | M03 |
| API 限流 | Lua 滑動窗口計數器 | M05 |
| 不重複訪客計數 | HyperLogLog（PFADD/PFCOUNT） | M03 |

#### 分散式模式

| 模式 | 實作方式 |
|------|---------|
| 分散式鎖 | SETNX + Lua 原子釋放 |
| 冪等性檢查 | SETNX + TTL |
| 全域唯一 ID | INCR + 時間戳 |

#### Redis 認證模擬考試

80 題涵蓋 7 大認證領域，及格門檻 80%：

| 領域 | 題數 | 涵蓋模組 |
|------|------|---------|
| Redis 通識 | 10 | M01 |
| Key 與過期 | 10 | M01-02 |
| 資料結構 | 15 | M02-03 |
| 資料建模 | 10 | M06, M11-12 |
| 除錯與疑難排解 | 10 | M13 |
| 效能最佳化 | 15 | M04-05, M13 |
| 叢集與高可用 | 10 | M08-10 |

**執行測試**：
```bash
./gradlew :module-14-capstone:test
```

---

## 共用模組（common）

`common/` 提供所有模組共用的基礎設施：

### 配置

- **RedisConfig** — `RedisTemplate` + Jackson 序列化（含 `JavaTimeModule`）
- **RedisKeyConvention** — Key 命名工具（`service:entity:id` 格式）

### 測試基底類別

```java
// 輕量 Redis（redis:8-alpine ~30MB），適用 M01-02, 04-10, 13
public abstract class AbstractRedisIntegrationTest { ... }

// 完整 Redis 8（含 Search / JSON / Bloom / TS 模組），適用 M03, 11, 12, 14
public abstract class AbstractRedisModuleIntegrationTest { ... }
```

兩者均採用 **Singleton Container 模式** — 同一 JVM 只啟動一個 Redis 容器，搭配 `@BeforeEach flushAll()` 保證測試隔離。

### Quiz 框架

每個模組附帶 Quiz 測驗，驗證理論知識：

```java
Quiz quiz = new Quiz("模組標題", "module-XX", questions, 0.8); // 80% 及格
QuizResult result = QuizRunner.run(quiz, userAnswers);
assertThat(result.passed()).isTrue();
```

## Docker 環境

### 基本環境

```bash
docker compose up -d
```

啟動的服務：

| 服務 | 埠號 | 說明 |
|------|------|------|
| Redis 8 | 6379 | 主要 Redis 實例 |
| RedisInsight | 5540 | Web 管理介面 |
| Prometheus | 9090 | 指標收集 |
| Grafana | 3000 | 視覺化儀表板（帳密 admin/admin） |

### Sentinel 環境

```bash
docker compose -f docker-compose-sentinel.yml up -d
```

| 服務 | 埠號 |
|------|------|
| Master | 6379 |
| Replica 1 | 6380 |
| Replica 2 | 6381 |
| Sentinel 1-3 | 26379-26381 |

### Cluster 環境

```bash
docker compose -f docker-compose-cluster.yml up -d
```

| 服務 | 埠號 |
|------|------|
| Node 1-6 | 7001-7006 |

## Gradle 指令速查

```bash
# 編譯全部
./gradlew build

# 執行全部測試
./gradlew test

# 執行單一模組測試
./gradlew :module-01-getting-started:test

# 重新執行測試（忽略快取）
./gradlew :module-01-getting-started:test --rerun

# 只編譯不測試
./gradlew build -x test

# 檢查依賴
./gradlew :module-01-getting-started:dependencies
```

## 常見問題

### Testcontainers 無法啟動容器

確認 Docker 正在運行，並檢查 `~/.testcontainers.properties`：

```properties
docker.host=unix:///var/run/docker.sock
testcontainers.reuse.enable=true
```

### Redis 模組指令（BF.\*、FT.\*、JSON.\*、TS.\*）報錯

本專案使用的 Docker Image：
- `redis:8-alpine` — 不含模組指令，僅適用基本操作
- `redis:8` — 包含全部模組（Search、JSON、Bloom、TimeSeries）

若整合測試的基底類別選錯，模組指令會失敗。需要模組指令的測試必須繼承 `AbstractRedisModuleIntegrationTest`。

### Lua 腳本中 Redis 8 RESP3 回傳格式問題

Redis 8 預設使用 RESP3 協定，浮點數在 Lua 中會以 `{ok = value}` 格式回傳。若遇到 `table: 0x...` 錯誤，需在 Lua 腳本中檢查 `type(val) == 'table'` 並提取 `val.ok`。

## 學習建議

1. **依序學習** — 從 Module 01 開始，每個模組建立在前一個的基礎上
2. **先讀測試** — 測試是最好的文件，先看測試理解預期行為
3. **動手實作** — 啟動 Docker 環境，用 `redis-cli` 手動練習指令
4. **理解架構** — 觀察六角形架構如何將業務邏輯與 Redis 操作分離
5. **挑戰 Quiz** — 完成每個模組的 Quiz 測驗，確認理論知識
6. **認證準備** — Module 14 的 80 題模擬考對應 Redis 認證七大領域

## 授權

本專案僅供教學與學習用途。
