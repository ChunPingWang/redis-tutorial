# Redis 實戰教學指南 — AI 開發工作清單 (Enhanced Edition)

> **參考來源**: [Redis University](https://university.redis.io/) 課程體系 (RU101, RU102J, RU301, Redis Streams, RediSearch, Redis Security)
> **技術棧**: Java 23, Spring Boot 4, Gradle (Kotlin DSL), Testcontainers, Redis Stack 7.4+
> **架構原則**: Domain-Driven Design (DDD), Hexagonal Architecture, SOLID Principles
> **領域範例**: 金融（銀行核心系統）、電商（訂單與庫存系統）
> **目標讀者**: 中高階 Java 開發人員與測試工程師
> **AI 開發模式**: 每個 Sub Module 為獨立可交付單元，含完整 Prompt 指引

---

## 課程對照 Redis University

本教學體系對齊 Redis University 官方課程，並加入企業級架構實踐：

| Redis University 課程 | 本教學對應模組 | 強化內容 |
|----------------------|---------------|---------|
| **Get Started with Redis** (原 RU101) | Module 01-02 | DDD 建模 + 六角形架構 |
| **Redis for Java Developers** (RU102J) | Module 01-06 | Spring Boot 4 + Lettuce (非 Jedis) + Redis OM Spring |
| **Redis Streams** | Module 07 | Event Sourcing + Consumer Group 企業模式 |
| **Running Redis at Scale** (RU301) | Module 08-10 | Testcontainers 模擬 + 金融級 RPO/RTO |
| **RediSearch / Query & Indexing** (RU201) | Module 11 | 全文檢索 + 電商搜尋引擎 |
| **RedisJSON / Document Store** | Module 12 | JSON 文件模型 + 複雜領域物件 |
| **Redis Security** | Module 13 | ACL + TLS + 金融合規 |
| **Vector Search** (新課程) | Module 12 (延伸) | 語意搜尋 + AI 整合 |
| — 綜合認證準備 — | Module 14 | Capstone + 認證模擬 |

---

## 全域專案結構

```
redis-tutorial/
├── settings.gradle.kts                    # 多模組根設定
├── build.gradle.kts                       # 共用 dependencies & plugins
├── gradle.properties
├── docker-compose.yml                     # Redis Stack (Redis + RedisInsight + Prometheus + Grafana)
├── docker-compose-cluster.yml             # Redis Cluster 6 節點
├── docker-compose-sentinel.yml            # Redis Sentinel 3 節點
│
├── module-01-getting-started/             # 入門：連線與基本操作
├── module-02-data-structures/             # 核心資料結構
├── module-03-specialized-structures/      # 進階資料結構 (Geo, Bitmap, HyperLogLog, Bloom)
├── module-04-caching-patterns/            # 快取模式與策略
├── module-05-pipelining-transactions/     # Pipeline, Transaction, Lua Script
├── module-06-data-modeling/               # Redis 資料建模模式 (DAO Pattern)
├── module-07-streams-events/              # Redis Streams 與事件驅動
├── module-08-persistence/                 # 持久化策略 (RDB + AOF)
├── module-09-high-availability/           # 複製 + Sentinel + 高可用
├── module-10-clustering/                  # Redis Cluster + 水平擴展
├── module-11-search-indexing/             # RediSearch 全文檢索與索引
├── module-12-json-vector/                 # RedisJSON + Vector Search
├── module-13-security-production/         # 安全、監控、生產最佳實踐
├── module-14-capstone/                    # 綜合實戰 + 認證準備
│
└── common/                                # 共用基礎設施
    ├── build.gradle.kts
    └── src/main/java/.../
        ├── test/AbstractRedisIntegrationTest.java
        ├── test/AbstractRedisStackIntegrationTest.java
        └── config/RedisStackConfig.java
```

---

## 共用基礎設施設定

### WL-000: 專案初始化與共用配置

| 項目 | 說明 |
|------|------|
| **目標** | 建立 Gradle 多模組專案骨架，統一依賴管理，對齊 Redis Stack 生態系 |
| **產出** | 專案骨架 + Docker Compose + 測試基底 + 架構慣例文件 |

#### 工作項目

- [ ] **WL-000-01** — 建立 `settings.gradle.kts`，註冊所有 14 個 sub module + common
- [ ] **WL-000-02** — 根 `build.gradle.kts` 配置共用依賴版本管理（BOM）

```kotlin
// build.gradle.kts — 核心依賴清單
dependencies {
    // Spring Boot 4.x
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Redis Client — Lettuce (官方推薦，非 Jedis)
    implementation("io.lettuce:lettuce-core:6.x")

    // Redis OM Spring — RedisJSON + RediSearch ORM
    implementation("com.redis.om:redis-om-spring:1.x")

    // Redis Stack Modules (via Jedis for module commands)
    implementation("redis.clients:jedis:7.x")  // 部分 module 指令需要

    // Redisson — 分散式鎖
    implementation("org.redisson:redisson-spring-boot-starter:3.x")

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("com.redis:testcontainers-redis:2.x")

    // Micrometer + Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus")
}
```

- [ ] **WL-000-03** — 建立 `docker-compose.yml`（Redis Stack 全功能）

```yaml
# docker-compose.yml
services:
  redis-stack:
    image: redis/redis-stack:7.4.0-v1
    ports:
      - "6379:6379"   # Redis
      - "8001:8001"   # RedisInsight UI
    volumes:
      - redis-data:/data
      - ./redis.conf:/redis-stack.conf
    environment:
      - REDIS_ARGS=--requirepass tutorial2025

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
```

- [ ] **WL-000-04** — 建立 Testcontainers 基底類別（兩種：基礎 Redis + Redis Stack）

```java
// AbstractRedisIntegrationTest.java — 基礎 Redis
@SpringBootTest
@Testcontainers
abstract class AbstractRedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379)
        .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeEach
    void flushRedis(@Autowired StringRedisTemplate redisTemplate) {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }
}

// AbstractRedisStackIntegrationTest.java — Redis Stack (含 Module)
@SpringBootTest
@Testcontainers
abstract class AbstractRedisStackIntegrationTest {

    @Container
    static GenericContainer<?> redisStack = new GenericContainer<>("redis/redis-stack:7.4.0-v1")
        .withExposedPorts(6379)
        .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisStack::getHost);
        registry.add("spring.data.redis.port", () -> redisStack.getMappedPort(6379));
    }
}
```

- [ ] **WL-000-05** — 建立共用六角形架構 Package 慣例文件
- [ ] **WL-000-06** — 建立 `RedisInsight` 使用指南（視覺化資料探索）
- [ ] **WL-000-07** — 建立各模組 Quiz 框架（對齊 Redis University 80% 通過率要求）

#### 六角形架構 Package 慣例（每個 Module 遵循）

```
com.tutorial.redis.module0X/
├── domain/
│   ├── model/                 # Entity, Value Object, Aggregate Root
│   ├── event/                 # Domain Event
│   ├── port/
│   │   ├── inbound/           # Use Case 介面 (Input Port)
│   │   └── outbound/          # Repository / Gateway 介面 (Output Port)
│   └── service/               # Domain Service
│
├── application/               # Application Service (Use Case 實作)
│   ├── usecase/
│   ├── dto/                   # Command / Query DTO
│   └── mapper/                # Domain ↔ DTO Mapper
│
├── adapter/
│   ├── inbound/
│   │   ├── rest/              # REST Controller
│   │   └── event/             # Event Listener (Redis Streams Consumer)
│   └── outbound/
│       ├── redis/             # Redis Adapter (RedisTemplate / Redis OM)
│       ├── persistence/       # JPA / H2 Adapter (模擬 DB)
│       └── messaging/         # Redis Pub/Sub / Streams Publisher
│
└── infrastructure/
    ├── config/                # Redis Config, Serializer Config
    └── exception/             # Exception Handler
```

#### AI Prompt 指引

```
你是一位資深 Java 架構師，精通 Redis 與 Spring Boot。
請建立一個 Gradle Kotlin DSL 多模組專案，包含：
- Java 23, Spring Boot 4, Redis Stack 7.4
- 14 個教學模組 + 1 個 common 模組
- docker-compose.yml: Redis Stack + RedisInsight + Prometheus + Grafana
- 兩個 Testcontainers 基底類別：基礎 Redis 與 Redis Stack
- Redis OM Spring 依賴配置
- 每個模組的六角形架構 Package 結構
- Quiz 測驗框架（JUnit-based，含計分與通過率檢查）
```

---

## Module 01 — Redis 入門與連線管理

### WL-010: Getting Started with Redis

> **對齊**: Redis University「Get Started with Redis」+ RU102J Chapter 1-2
> **學習時間**: 3-4 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | 圖表 #1 In-Memory Storage, #2 Single-Threaded, #3 I/O Multiplexing |
| **領域範例** | 金融：帳戶餘額快取 / 電商：商品基本資訊快取 |
| **學習目標** | Redis 記憶體模型、redis-cli 操作、Lettuce 連線池、Spring Data Redis Template |

#### 工作項目

- [ ] **WL-010-01** — **Redis 伺服器概觀** (對齊 RU301 §1.0)
  - Redis 架構：Single-threaded + I/O Multiplexing (epoll/kqueue)
  - 記憶體模型與 sub-millisecond 延遲原理
  - 文件產出：Redis Architecture Overview diagram
- [ ] **WL-010-02** — **redis-cli 基礎操作** (對齊 RU301 §1.1)
  - `SET` / `GET` / `DEL` / `EXISTS` / `EXPIRE` / `TTL` / `TYPE`
  - `KEYS` vs `SCAN` 的差異與風險
  - `INFO` / `CONFIG GET` / `CONFIG SET` 伺服器狀態查看
  - 練習：使用 RedisInsight GUI 對照 CLI 操作
- [ ] **WL-010-03** — **Redis 配置管理** (對齊 RU301 §1.2)
  - `redis.conf` 核心參數：`maxmemory`, `maxmemory-policy`, `bind`, `protected-mode`
  - 運行時動態配置 `CONFIG SET` vs 靜態配置
  - Testcontainers 自訂 `redis.conf` 掛載
- [ ] **WL-010-04** — **Lettuce 連線管理** (對齊 RU301 §1.3-1.4)
  - Lettuce vs Jedis 比較：非阻塞 I/O, 執行緒安全, 連線共享
  - `LettuceConnectionFactory` 配置
  - 連線池參數調校：`maxTotal`, `maxIdle`, `minIdle`, `testOnBorrow`
  - Standalone / Sentinel / Cluster 三種模式連線字串
  - **Client Performance**: 連線池大小計算公式
- [ ] **WL-010-05** — **Spring Data Redis Template** 基礎
  - `StringRedisTemplate` vs `RedisTemplate<String, T>`
  - Jackson JSON Serializer 配置
  - `ValueOperations`, `HashOperations`, `ListOperations` 等 Operations 介面
- [ ] **WL-010-06** — **領域建模與六角形架構實作**
  - Domain: `Account(accountId, holderName, balance, currency, lastUpdated)`
  - Domain: `Product(productId, name, price, category, stockQuantity)`
  - Outbound Port: `AccountCachePort(save, findById, evict, exists)`
  - Outbound Port: `ProductCachePort(save, findById, evict, findByIds)`
  - Adapter: `RedisAccountCacheAdapter` — 使用 `RedisTemplate`
  - Key 命名策略：`{domain}:{entity}:{id}` (e.g., `banking:account:ACC-001`)
- [ ] **WL-010-07** — **Inbound Use Case 實作**
  - `GetAccountBalanceUseCase` / `CacheProductUseCase`
  - Application Service 編排 Cache Port + (模擬) DB Port
- [ ] **WL-010-08** — **Initial Tuning** (對齊 RU301 §1.5)
  - `tcp-backlog`, `tcp-keepalive`, `timeout` 參數
  - Lettuce 的 `clientOptions` 與 `socketOptions` 調校
- [ ] **WL-010-09** — **測試**: Testcontainers 整合測試
  - 連線建立與斷線自動重連
  - 基本 CRUD 操作
  - TTL 過期行為驗證
  - Key 命名慣例驗證（regex pattern match）
- [ ] **WL-010-10** — **測試**: 單元測試（Mock Redis Port）
  - Use Case 業務邏輯隔離測試
  - Port 介面行為契約測試
- [ ] **WL-010-11** — **Quiz**: 模組測驗（10 題，需 80% 通過）

#### AI Prompt 指引

```
請為 module-01 實作 Redis 入門教學。對齊 Redis University RU101 + RU301 Chapter 1。
要求：
1. 先建立 redis-cli 操作範例腳本（.sh），涵蓋基礎命令 + INFO + CONFIG
2. Domain Layer: Account (金融) + Product (電商) Value Object，不可變
3. Port/Adapter: AccountCachePort → RedisAccountCacheAdapter
4. Config: Lettuce 連線池完整配置，含三種模式（Standalone/Sentinel/Cluster）
5. 調校參數：tcp-backlog, keepalive, clientOptions
6. Test: Testcontainers 整合測試 (CRUD + TTL + 重連)
7. Test: 單元測試 (Mock Port, 驗證 UseCase)
8. Quiz: 10 題選擇題，涵蓋 redis-cli、連線池、記憶體模型
所有類別遵循 SOLID，Domain Layer 零框架依賴。
```

---

## Module 02 — 核心資料結構

### WL-020: Core Data Structures

> **對齊**: Redis University RU101 — Strings, Hashes, Lists, Sets, Sorted Sets
> **學習時間**: 5-6 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | 圖表 #4 Data Structures (Hash Table, Skip List, Ziplist) |
| **領域範例** | 金融：交易排行榜 / 電商：購物車、庫存、用戶收藏 |
| **學習目標** | 掌握 5 大核心資料結構的命令、時間複雜度、內部編碼與最佳場景 |

#### 工作項目

- [ ] **WL-020-01** — **String**: 電商即時庫存計數器
  - 命令：`SET`, `GET`, `INCR`, `DECRBY`, `MGET`, `MSET`, `SETNX`, `SETEX`
  - 時間複雜度：O(1)
  - 內部編碼：`int` (數字) / `embstr` (≤44 bytes) / `raw` (>44 bytes)
  - Domain: `StockLevel(productId, quantity)`
  - Port: `StockLevelPort(increment, decrement, getLevel, batchGet)`
  - **RU101 模式**: Capped Collection — `INCR` + `EXPIRE` 組合計數器
- [ ] **WL-020-02** — **Hash**: 電商購物車
  - 命令：`HSET`, `HGET`, `HDEL`, `HGETALL`, `HMSET`, `HINCRBY`, `HEXISTS`
  - 時間複雜度：O(1) per field, O(N) for HGETALL
  - 內部編碼：`listpack` (小集合, ≤128 fields) / `hashtable` (大集合)
  - Domain: `ShoppingCart(customerId, Map<ProductId, CartItem>)`
  - Key: `ecommerce:cart:{customerId}`, Field: `{productId}`
  - **RU101 模式**: Partial Update — 只更新單一 field 而非整個物件
- [ ] **WL-020-03** — **List**: 金融交易歷史（Capped Collection）
  - 命令：`LPUSH`, `RPUSH`, `LPOP`, `LRANGE`, `LTRIM`, `LLEN`, `LINDEX`
  - 時間複雜度：O(1) 兩端操作, O(N) LRANGE
  - 內部編碼：`listpack` (小) / `quicklist` (大)
  - Domain: `TransactionLog(accountId, List<Transaction>)`
  - **RU101 模式**: Capped List — `LPUSH` + `LTRIM` 保留最近 N 筆
  - 電商延伸：最近瀏覽商品紀錄
- [ ] **WL-020-04** — **Set**: 電商商品標籤與社群推薦
  - 命令：`SADD`, `SREM`, `SMEMBERS`, `SISMEMBER`, `SINTER`, `SUNION`, `SDIFF`, `SRANDMEMBER`
  - 時間複雜度：O(1) per element, O(N*M) for SINTER
  - 內部編碼：`listpack` (小, ≤128) / `hashtable` (大)
  - **RU101 模式**: Set Operations — 共同好友、共同收藏商品
  - Use Case: `SINTER user:123:favorites user:456:favorites` → 推薦引擎
- [ ] **WL-020-05** — **Sorted Set**: 金融交易金額排行榜
  - 命令：`ZADD`, `ZREM`, `ZSCORE`, `ZRANK`, `ZREVRANK`, `ZRANGE`, `ZRANGEBYSCORE`, `ZINCRBY`, `ZCOUNT`
  - 時間複雜度：O(log N) 大部分操作
  - 內部編碼：`listpack` (小) / `skiplist` + `hashtable` (大)
  - Domain: `TransactionRanking(timeWindow, List<RankEntry>)`
  - **RU101 模式**: Leaderboard — 即時排行榜
  - **RU102J 模式**: Capacity Ranking (Solar Dashboard) → 改為交易排行
  - 金融 Use Case: Top-N 當日最大交易、即時匯率排序
  - 電商 Use Case: 熱銷商品排行、用戶積分排行
- [ ] **WL-020-06** — **Key 過期與管理策略**
  - `EXPIRE`, `PEXPIRE`, `EXPIREAT`, `PERSIST`, `TTL`, `PTTL`
  - Lazy Expiration vs Active Expiration 機制
  - Redis 7.4 新功能：**Hash Field Expiration**（`HEXPIRE`）
  - Key Space Notification 配置
- [ ] **WL-020-07** — **資料結構時間複雜度速查表**（文件產出）

  | Structure | Add | Remove | Lookup | Range | Memory |
  |-----------|-----|--------|--------|-------|--------|
  | String | O(1) | O(1) | O(1) | — | Low |
  | Hash | O(1) | O(1) | O(1) | O(N) | Medium |
  | List | O(1)* | O(1)* | O(N) | O(S+N) | Medium |
  | Set | O(1) | O(1) | O(1) | O(N) | Medium |
  | Sorted Set | O(log N) | O(log N) | O(1) | O(log N + M) | High |

- [ ] **WL-020-08** — **測試**: 每種結構整合測試 (`@Nested` class)
  - 基本 CRUD
  - 邊界條件：空集合、大量資料 (10K+)、併發操作
  - 內部編碼切換驗證（`OBJECT ENCODING`）
- [ ] **WL-020-09** — **Quiz**: 資料結構選型測驗（15 題，80% 通過）

#### AI Prompt 指引

```
請為 module-02 實作 Redis 核心資料結構教學。對齊 Redis University RU101。
要求：
1. 五種結構各完整實作: Domain Model → Port → Adapter → Test
2. 每種結構標註時間複雜度與內部編碼
3. Sorted Set: 金融排行榜 + 電商熱銷榜，對齊 RU102J CapacityDao 模式
4. Hash: 購物車，展示 Partial Update 優勢
5. List: Capped Collection (LPUSH + LTRIM)
6. Set: 集合運算推薦引擎 (SINTER/SUNION)
7. 含 Redis 7.4 Hash Field Expiration 新功能
8. 時間複雜度速查表 markdown
9. @Nested 測試，每種結構至少 5 個案例
```

---

## Module 03 — 進階資料結構

### WL-030: Specialized Data Structures

> **對齊**: Redis University RU101 進階章節 — Geospatial, Bitmap, HyperLogLog, Bloom Filter
> **學習時間**: 4-5 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | RU101 Specialized Structures + Redis Stack Bloom Filter |
| **領域範例** | 金融：用戶活躍分析 / 電商：附近門市、UV 統計、防重複優惠券 |
| **學習目標** | 機率型資料結構、地理空間查詢、位元操作 |

#### 工作項目

- [ ] **WL-030-01** — **Geospatial Index**: 電商附近門市/取貨點
  - 命令：`GEOADD`, `GEODIST`, `GEORADIUS`, `GEOSEARCH`, `GEOPOS`
  - 基於 Sorted Set + Geohash 實作原理
  - Domain: `StoreLocation(storeId, name, longitude, latitude)`
  - Use Case: 查詢用戶 5km 內的取貨點
  - 金融延伸：ATM 位置查詢
- [ ] **WL-030-02** — **Bitmap**: 金融用戶簽到/活躍天數
  - 命令：`SETBIT`, `GETBIT`, `BITCOUNT`, `BITOP`, `BITPOS`, `BITFIELD`
  - Domain: `UserActivity(userId, year, month)`
  - Key: `banking:activity:{userId}:{yearMonth}`
  - Use Case 1: 計算月活躍天數 (`BITCOUNT`)
  - Use Case 2: 連續簽到天數計算
  - Use Case 3: `BITOP AND` 計算兩個時段都活躍的用戶
- [ ] **WL-030-03** — **HyperLogLog**: 電商 UV 統計
  - 命令：`PFADD`, `PFCOUNT`, `PFMERGE`
  - 0.81% 標準誤差，12KB 固定記憶體
  - Domain: `UniqueVisitorCounter(pageId, date)`
  - Use Case: 每日/每週/每月不重複訪客估算
  - `PFMERGE` 合併多天數據
- [ ] **WL-030-04** — **Bloom Filter** (Redis Stack / RedisBloom): 防重複
  - 命令：`BF.ADD`, `BF.EXISTS`, `BF.MADD`, `BF.MEXISTS`, `BF.RESERVE`
  - 假陽性率 (False Positive Rate) 配置
  - 金融 Use Case: 快速過濾已處理的交易 ID（防重複扣款第一層）
  - 電商 Use Case: 優惠券是否已被領取
  - Port: `BloomFilterPort(add, mightContain, addAll)`
- [ ] **WL-030-05** — **Cuckoo Filter** (Redis Stack): 支援刪除的機率過濾器
  - `CF.ADD`, `CF.EXISTS`, `CF.DEL`
  - 與 Bloom Filter 的比較決策
- [ ] **WL-030-06** — **TimeSeries** (Redis Stack / RedisTimeSeries): 金融時序資料
  - 命令：`TS.CREATE`, `TS.ADD`, `TS.RANGE`, `TS.MRANGE`, `TS.CREATERULE`
  - 自動降採樣（Downsampling）與資料保留策略
  - 金融 Use Case: 即時股價/匯率時序存儲
  - 電商 Use Case: 訂單量時序監控
- [ ] **WL-030-07** — **測試**: 各特殊結構整合測試
  - Geo: 距離計算精確度驗證
  - Bitmap: 大範圍 bit 操作效能
  - HyperLogLog: 大量元素的誤差率驗證
  - Bloom Filter: 假陽性率統計驗證（插入 100K，檢測偏差）
- [ ] **WL-030-08** — **Quiz**: 特殊結構場景選型測驗（10 題，80% 通過）

#### AI Prompt 指引

```
請為 module-03 實作 Redis 進階資料結構教學。對齊 RU101 進階章節 + Redis Stack 模組。
要求：
1. Geospatial: 電商附近門市查詢，含座標資料匯入
2. Bitmap: 金融用戶活躍天數，含連續簽到計算邏輯
3. HyperLogLog: UV 統計，展示 PFMERGE 合併多天數據
4. Bloom Filter (RedisBloom): 防重複扣款，含假陽性率統計測試
5. TimeSeries (RedisTimeSeries): 金融即時匯率存儲 + 降採樣
6. 使用 AbstractRedisStackIntegrationTest 基底（需要 Redis Stack image）
7. Bloom Filter 測試：插入 100K 元素，統計假陽性率是否在預期範圍
```

---

## Module 04 — 快取模式與策略

### WL-040: Caching Patterns

> **對齊**: RU102J Cache 章節 + 業界最佳實踐
> **學習時間**: 5-6 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | 圖表 #1 In-Memory Storage（快取層設計）|
| **領域範例** | 金融：匯率快取 / 電商：商品目錄快取 |
| **學習目標** | 5 種快取模式、快取三大問題防護、Spring Cache 抽象 |

#### 工作項目

- [ ] **WL-040-01** — **Cache-Aside (Lazy Loading)** Pattern
  - 金融：匯率查詢服務
  - 流程：App → Cache (Miss?) → DB → Write Cache → Return
  - Port 分離：`ExchangeRateCachePort` + `ExchangeRateRepositoryPort`
  - Domain Service: 先查 Cache，Miss 時查 DB，回寫 Cache
- [ ] **WL-040-02** — **Read-Through / Write-Through** Pattern
  - 電商：商品目錄服務
  - Spring `@Cacheable` / `@CachePut` / `@CacheEvict` 配置
  - Custom `RedisCacheManager` with TTL per cache name
  - `@CacheConfig(cacheNames = "products")` 類別級配置
- [ ] **WL-040-03** — **Write-Behind (Write-Back)** Pattern
  - 金融：交易日誌批次寫入（減少 DB 壓力）
  - Redis List 暫存 → `@Scheduled` 批次 flush → DB
  - 配置：flush 週期、batch size、error handling
- [ ] **WL-040-04** — **Refresh-Ahead** Pattern
  - 電商：熱門商品快取主動刷新
  - TTL 剩餘不足 20% 時觸發非同步刷新
  - 避免 Cache Miss 造成的延遲抖動
- [ ] **WL-040-05** — **Multi-Level Cache** (L1 Local + L2 Redis)
  - Caffeine (Local) + Redis (Remote) 多層快取
  - 一致性維護策略
- [ ] **WL-040-06** — **快取失效策略**
  - TTL-based expiry（固定 / 滑動窗口）
  - Event-based eviction（模擬 CDC / Domain Event 觸發）
  - 版本號策略 (ETag-like)
- [ ] **WL-040-07** — **快取三大問題防護**

  | 問題 | 成因 | 防護方案 | 實作 |
  |------|------|---------|------|
  | **穿透** | 查詢不存在的 Key | Bloom Filter / Null Object Cache | `BF.EXISTS` 前置過濾 |
  | **擊穿** | 熱點 Key 失效瞬間 | 分散式鎖 + 邏輯過期 | `SETNX` 保護重建 |
  | **雪崩** | 大量 Key 同時失效 | TTL 隨機化 + 多層快取 | `TTL = base + random(0, spread)` |

- [ ] **WL-040-08** — **測試**: 各模式整合測試
  - Cache Hit / Miss 路徑驗證
  - TTL 過期行為
  - Write-Behind flush 完整性
  - 併發 Cache Stampede 測試（多執行緒同時觸發 Miss）
- [ ] **WL-040-09** — **Quiz + 決策矩陣**

  | Pattern | Consistency | Latency | Complexity | Best For |
  |---------|------------|---------|------------|----------|
  | Cache-Aside | Eventual | Medium | Low | 一般查詢 |
  | Read-Through | Eventual | Low | Medium | 讀多寫少 |
  | Write-Through | Strong | High | Medium | 一致性要求 |
  | Write-Behind | Eventual | Low | High | 寫入密集 |
  | Refresh-Ahead | Near-RT | Low | High | 熱點數據 |

#### AI Prompt 指引

```
請為 module-04 實作 5 種快取模式 + 三大問題防護。對齊 RU102J。
核心要求：
1. Cache-Aside: 金融匯率，Port 分離 Cache 與 DB
2. Read-Through: Spring @Cacheable + 自訂 RedisCacheManager
3. Write-Behind: Redis List + @Scheduled batch flush
4. Refresh-Ahead: TTL 不足 20% 時非同步刷新
5. Multi-Level: Caffeine L1 + Redis L2
6. 快取穿透：Bloom Filter (RedisBloom) 前置過濾
7. 快取擊穿：SETNX 分散式鎖保護熱點 Key 重建
8. 快取雪崩：TTL 隨機化公式實作
9. 併發測試：10 threads 同時觸發 Cache Miss
10. 產出快取模式決策矩陣表
```

---

## Module 05 — Pipeline 與交易

### WL-050: Pipelining & Transactions

> **對齊**: RU102J — Pipelining, Transactions, Lua Scripting
> **學習時間**: 4-5 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | 圖表 #5 Pipelining (1 RTT vs 3 RTTs) |
| **領域範例** | 金融：批次帳戶餘額更新、轉帳 / 電商：批次庫存扣減 |
| **學習目標** | Pipeline 批次、MULTI/EXEC 交易、Lua Script 原子操作、WATCH CAS |

#### 工作項目

- [ ] **WL-050-01** — **Pipeline** 批次操作 (對齊 RU102J Pipeline 章節)
  - 電商：批次查詢 100/1000/10000 個商品價格
  - `RedisTemplate.executePipelined(RedisCallback<?>)` 實作
  - 效能對比：逐筆 vs Pipeline 的 RTT 差異
  - **RU102J 模式**: `CapacityDaoRedisImpl` → 改為批次價格查詢
- [ ] **WL-050-02** — **MULTI/EXEC Transaction** (對齊 RU102J Transaction 章節)
  - 金融：帳戶間轉帳（A 扣款 + B 入帳原子操作）
  - `SessionCallback` 實作 MULTI/EXEC
  - 注意：Transaction 內命令排隊但不會立即執行
  - 錯誤處理：EXEC 返回 null（WATCH 觸發放棄）
- [ ] **WL-050-03** — **Optimistic Locking with WATCH**
  - 金融：CAS (Compare-And-Set) 更新帳戶餘額
  - WATCH → GET → (檢查) → MULTI → SET → EXEC retry loop
  - 重試策略：指數退避 + 最大重試次數
- [ ] **WL-050-04** — **Lua Script 原子操作** (對齊 RU101 Lua 章節)
  - 電商：庫存檢查 + 扣減原子操作（防超賣）
  - `DefaultRedisScript<Long>` 配置
  - Lua Script 載入與 SHA 快取 (`EVALSHA`)
  - Domain Port: `AtomicStockDeductionPort`

  ```lua
  -- check_and_deduct.lua
  local stock = tonumber(redis.call('GET', KEYS[1]))
  if stock == nil then return -1 end
  if stock < tonumber(ARGV[1]) then return 0 end
  redis.call('DECRBY', KEYS[1], ARGV[1])
  return 1
  ```

- [ ] **WL-050-05** — **Pipeline vs Transaction vs Lua 決策**

  | 特性 | Pipeline | MULTI/EXEC | Lua Script |
  |------|----------|------------|------------|
  | 原子性 | ✗ | ✓ (弱) | ✓ (強) |
  | 條件邏輯 | ✗ | ✗ | ✓ |
  | 減少 RTT | ✓ | ✓ | ✓ |
  | 阻塞其他命令 | ✗ | ✗ | ✓ |
  | 適用場景 | 批次讀寫 | 簡單原子組合 | 複雜原子邏輯 |

- [ ] **WL-050-06** — **測試**: Pipeline 效能基準
  - 100 / 1,000 / 10,000 筆操作延遲對比
  - 產出效能數據（可用於 Chart）
- [ ] **WL-050-07** — **測試**: Transaction 原子性驗證
  - 併發轉帳（10 threads × 100 次），驗證總餘額守恆
  - WATCH 衝突率統計
- [ ] **WL-050-08** — **測試**: Lua Script 超賣防護
  - 100 庫存 × 200 併發扣減 → 驗證不超賣
- [ ] **WL-050-09** — **Quiz**: 10 題（80% 通過）

#### AI Prompt 指引

```
請為 module-05 實作 Pipeline、Transaction、Lua Script 教學。對齊 RU102J。
核心場景：
1. Pipeline: 對齊 RU102J CapacityDao 模式，批次查詢 + 效能 benchmark
2. MULTI/EXEC: 金融轉帳 + WATCH 樂觀鎖 + 指數退避重試
3. Lua Script: 庫存原子扣減 (check_and_deduct.lua)，EVALSHA 快取
4. 決策比較表：Pipeline vs Transaction vs Lua
5. 併發測試：轉帳餘額守恆 + 庫存不超賣
6. 所有操作封裝在 Outbound Port（六角形架構）
```

---

## Module 06 — Redis 資料建模模式

### WL-060: Data Modeling Patterns

> **對齊**: RU102J — DAO Design Patterns, Time-Series Data Modeling
> **學習時間**: 4-5 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | RU102J Application Building + DDD Data Modeling |
| **領域範例** | 金融：完整帳戶聚合 / 電商：訂單聚合 |
| **學習目標** | Redis 資料建模範式、Key Schema 設計、聚合存儲策略 |

#### 工作項目

- [ ] **WL-060-01** — **Key Schema 設計規範**
  - 命名慣例：`{service}:{entity}:{id}[:{field}]`
  - 分隔符號選擇：`:` (標準)
  - Key 長度控制（權衡可讀性與記憶體）
  - 命名空間管理：避免跨服務 Key 衝突
- [ ] **WL-060-02** — **Aggregate 存儲模式**
  - 模式 A: 單一 Key + JSON String（簡單，整體讀寫）
  - 模式 B: Hash Per Entity（部分更新，較省記憶體）
  - 模式 C: 多 Key 組合（關聯查詢，靈活度高）
  - 金融範例：Account Aggregate 三種方式對比
- [ ] **WL-060-03** — **DAO Pattern** (對齊 RU102J)
  - 仿 RU102J 的 `SiteDaoRedisImpl` 模式
  - 改為六角形架構：Domain Port → Redis Adapter
  - 金融 DAO: `AccountRedisAdapter`（CRUD + Search by criteria）
  - 電商 DAO: `OrderRedisAdapter`（建立 + 狀態變更 + 查詢）
- [ ] **WL-060-04** — **Secondary Index 手動建立**
  - 使用 Set 建立二級索引：`idx:account:currency:USD` → `{accountId set}`
  - 使用 Sorted Set 建立時間索引：`idx:order:created` → `{orderId, timestamp}`
  - 維護策略：寫入時同步更新索引
- [ ] **WL-060-05** — **Time-Series Data Modeling** (對齊 RU102J)
  - 金融：每分鐘匯率快照
  - Key 策略：`rate:{pair}:{YYYYMMDD}` + Sorted Set (score=timestamp)
  - 查詢模式：時間範圍查詢 (`ZRANGEBYSCORE`)
  - 資料過期策略：每日 Key 自動過期
- [ ] **WL-060-06** — **Reference Pattern**: 跨 Aggregate 關聯
  - 電商：Order → Customer (Reference by ID)
  - 反正規化 vs 引用的取捨
- [ ] **WL-060-07** — **測試**: 資料建模整合測試
  - Aggregate CRUD 完整路徑
  - 二級索引一致性驗證
  - 時序資料範圍查詢精確度
- [ ] **WL-060-08** — **文件產出**: Redis Key Schema Design Guide

#### AI Prompt 指引

```
請為 module-06 實作 Redis 資料建模教學。對齊 RU102J DAO 設計模式。
要求：
1. Key Schema 設計規範文件
2. Aggregate 三種存儲模式比較 (JSON String / Hash / Multi-Key)
3. DAO Pattern: 對齊 RU102J SiteDaoRedisImpl，改為六角形架構
4. Secondary Index: Set + Sorted Set 手動索引
5. Time-Series: 金融匯率快照，ZRANGEBYSCORE 時間查詢
6. 金融 Account Aggregate + 電商 Order Aggregate 完整建模
7. 測試：二級索引一致性、時序查詢精確度
```

---

## Module 07 — Redis Streams 與事件驅動

### WL-070: Streams & Event-Driven Architecture

> **對齊**: Redis University — Redis Streams 完整課程
> **學習時間**: 6-7 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | Redis Streams — 最強大的資料結構 |
| **領域範例** | 金融：即時風控告警 / 電商：訂單狀態變更事件 |
| **學習目標** | Streams CRUD、Consumer Group、Pub/Sub 比較、Event Sourcing |

#### 工作項目

- [ ] **WL-070-01** — **Pub/Sub** 基礎（對比 Streams）
  - 命令：`PUBLISH`, `SUBSCRIBE`, `PSUBSCRIBE`
  - `RedisMessageListenerContainer` 配置
  - 金融：即時匯率廣播
  - **限制**: Fire-and-forget, 無持久化, 訂閱者離線即遺失
- [ ] **WL-070-02** — **Streams 基礎概念** (對齊 Redis Streams 課程)
  - 概念：Append-only log, Message ID (timestamp-sequence)
  - 命令：`XADD`, `XLEN`, `XRANGE`, `XREVRANGE`, `XREAD`, `XTRIM`
  - 與 Kafka Topics 的概念對比
  - Domain Event: `OrderStatusChangedEvent(orderId, fromStatus, toStatus, timestamp)`
- [ ] **WL-070-03** — **Consumer Group** (對齊 Redis Streams 課程核心)
  - 命令：`XGROUP CREATE`, `XREADGROUP`, `XACK`, `XPENDING`, `XCLAIM`
  - 多個消費者分擔處理（Load Balancing）
  - 消息確認機制：至少一次語意 (At-Least-Once)
  - Pending Entry List (PEL): 追蹤未確認消息
- [ ] **WL-070-04** — **Dead Letter 處理**
  - `XPENDING` 查詢長時間未確認消息
  - `XCLAIM` 重新分配給其他消費者
  - 超過最大重試次數 → 轉入 Dead Letter Stream
  - 自訂 `DeadLetterHandler` 元件
- [ ] **WL-070-05** — **Spring Data Redis Stream Support**
  - `StreamMessageListenerContainer` 配置
  - `StreamListener<String, MapRecord<String, String, String>>` 實作
  - `StreamReceiver` for reactive streams
  - 自動 ACK vs 手動 ACK 配置
- [ ] **WL-070-06** — **Event Sourcing Pattern**
  - 金融：帳戶事件溯源
  - Events: `AccountOpened`, `MoneyDeposited`, `MoneyWithdrawn`, `AccountFrozen`
  - Redis Stream 作為 Event Store
  - Event Replay: 從 Stream 重建 Aggregate 狀態
  - Port: `EventStorePort(append, readAll, readFrom)`
- [ ] **WL-070-07** — **Pub/Sub vs Streams 決策**

  | 特性 | Pub/Sub | Streams |
  |------|---------|---------|
  | 持久化 | ✗ | ✓ |
  | 消費者群組 | ✗ | ✓ |
  | 回溯消費 | ✗ | ✓ |
  | 消息確認 | ✗ | ✓ (ACK) |
  | 效能 | 極高 | 高 |
  | 適用場景 | 即時廣播 | 可靠消息處理 |

- [ ] **WL-070-08** — **測試**: Pub/Sub 訊息送達
  - 多訂閱者同時接收驗證
  - 斷線後消息遺失驗證
- [ ] **WL-070-09** — **測試**: Consumer Group 測試
  - 消息分配均衡性
  - ACK / Pending 狀態
  - 消費者 crash → XCLAIM 接手
  - Dead Letter 轉移
- [ ] **WL-070-10** — **測試**: Event Sourcing Replay
  - 寫入 10 個事件 → Replay → 驗證最終狀態
- [ ] **WL-070-11** — **Quiz**: 15 題（80% 通過）

#### AI Prompt 指引

```
請為 module-07 實作 Redis Streams 完整教學。對齊 Redis University Streams 課程。
要求：
1. Pub/Sub: 金融匯率廣播，展示 fire-and-forget 限制
2. Streams 基礎: XADD/XREAD/XRANGE，電商訂單事件
3. Consumer Group: XREADGROUP + XACK + XPENDING + XCLAIM
4. Dead Letter: 超過重試 → 轉移到 DLQ Stream
5. Spring Data: StreamMessageListenerContainer 配置
6. Event Sourcing: 帳戶事件溯源，Stream 作為 Event Store
7. Pub/Sub vs Streams 比較表
8. 六角形架構：EventStorePort / EventPublisherPort / EventSubscriberPort
9. 測試：Consumer Group 分配 + XCLAIM + Replay
```

---

## Module 08 — 持久化策略

### WL-080: Persistence & Durability

> **對齊**: RU301 Chapter 2 — Persistence & Durability
> **學習時間**: 3-4 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | 圖表 #6 Persistence (Fork, RDB Snapshot, AOF Log) |
| **領域範例** | 金融：交易資料零遺失保證 |
| **學習目標** | RDB, AOF, Hybrid 機制、RPO/RTO 分析、資料恢復 |

#### 工作項目

- [ ] **WL-080-01** — **RDB Snapshot** (對齊 RU301 §2.1-2.2)
  - Fork + Copy-on-Write 機制
  - 配置：`save 900 1`, `save 300 10`, `save 60 10000`
  - `BGSAVE` / `BGREWRITEAOF` 手動觸發
  - Testcontainers 自訂 `redis.conf` 掛載驗證
- [ ] **WL-080-02** — **AOF Logging** (對齊 RU301 §2.1)
  - `appendonly yes`
  - `appendfsync` 三種策略：`always` / `everysec` / `no`
  - AOF Rewrite 機制與觸發條件
  - AOF 檔案增長觀測
- [ ] **WL-080-03** — **Hybrid Persistence** (RDB + AOF)
  - `aof-use-rdb-preamble yes`
  - 結合 RDB 的快速載入 + AOF 的低資料遺失
- [ ] **WL-080-04** — **RPO/RTO 分析** (金融場景)

  | 策略 | RPO (最大資料遺失) | RTO (恢復時間) | 效能影響 | 適用場景 |
  |------|-------------------|---------------|---------|---------|
  | RDB only | 分鐘級 | 快 (載入 dump) | 低 | 可容忍資料遺失 |
  | AOF always | 0 (每筆 fsync) | 慢 (重播 log) | 高 | 金融交易 |
  | AOF everysec | ~1 秒 | 慢 | 中 | 一般業務 |
  | Hybrid | ~1 秒 | 中 | 中 | **推薦預設** |

- [ ] **WL-080-05** — **資料恢復模擬**
  - 寫入測試資料 → Kill Redis → 重啟 → 驗證完整性
  - RDB 恢復 vs AOF 恢復 對比
  - Testcontainers 控制容器生命週期
- [ ] **WL-080-06** — **測試**: 持久化恢復整合測試
- [ ] **WL-080-07** — **測試**: 效能基準 (RDB vs AOF vs Hybrid 寫入 throughput)
- [ ] **WL-080-08** — **文件**: 金融系統持久化策略建議書
- [ ] **WL-080-09** — **Quiz**: 8 題（80% 通過）

---

## Module 09 — 高可用架構

### WL-090: High Availability

> **對齊**: RU301 Chapter 3 — High Availability (Replication + Sentinel)
> **學習時間**: 5-6 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | 圖表 #7 Replication (Master-Replica, Async) |
| **領域範例** | 金融：讀寫分離、自動故障轉移 |
| **學習目標** | Basic Replication, Sentinel 機制, Read-Your-Writes |

#### 工作項目

- [ ] **WL-090-01** — **Basic Replication** (對齊 RU301 §3.1-3.2)
  - `docker-compose-sentinel.yml`: 1 Master + 2 Replica
  - `REPLICAOF` 配置
  - 異步複製機制：Replication Offset, Backlog
  - `INFO replication` 監控
- [ ] **WL-090-02** — **Lettuce 讀寫分離**
  - `ReadFrom.REPLICA_PREFERRED` / `ReadFrom.REPLICA` / `ReadFrom.ANY_REPLICA`
  - 金融場景：報表查詢走 Replica，交易寫入走 Master
- [ ] **WL-090-03** — **Replication Lag 問題**
  - 金融場景：轉帳後立即查餘額（讀到舊值）
  - 解決方案 1: Read-Your-Writes（寫後短時間讀 Master）
  - 解決方案 2: `WAIT` 命令（同步等待 Replica 確認）
  - 解決方案 3: 版本號比對
- [ ] **WL-090-04** — **Redis Sentinel** (對齊 RU301 §3.3-3.4)
  - Sentinel 架構：3 個 Sentinel 節點監控
  - 自動故障偵測：`SDOWN` → `ODOWN` 投票
  - 自動故障轉移：選舉新 Master
  - Lettuce Sentinel 配置
  - `docker-compose-sentinel.yml` 完整配置
- [ ] **WL-090-05** — **Failover 模擬** (對齊 RU301 Exercise)
  - Kill Master → 觀察 Sentinel 日誌 → 驗證新 Master 選舉
  - 應用層自動重連驗證
  - 資料完整性驗證
- [ ] **WL-090-06** — **測試**: 讀寫分離整合測試
- [ ] **WL-090-07** — **測試**: Sentinel Failover 測試
  - Testcontainers 控制 Master 停機
  - 驗證自動切換 + 資料完整性
- [ ] **WL-090-08** — **Quiz**: 10 題（80% 通過）

---

## Module 10 — Redis Cluster 與水平擴展

### WL-100: Clustering & Horizontal Scaling

> **對齊**: RU301 Chapter 4 — Scalability (Redis Cluster)
> **學習時間**: 5-6 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | 圖表 #8 Clustering (CRC16 Hash, 16384 Slots) |
| **領域範例** | 電商：跨區域大規模部署 |
| **學習目標** | Cluster 架構、Hash Slot、節點管理、多 Key 操作限制 |

#### 工作項目

- [ ] **WL-100-01** — **Redis Cluster 架構** (對齊 RU301 §4.0)
  - 16,384 Hash Slots 分配機制
  - CRC16 Hash → Slot → Node 映射
  - Gossip 協議：節點間通信
  - `docker-compose-cluster.yml`: 6 節點 (3 Master + 3 Replica)
- [ ] **WL-100-02** — **Cluster 建立與管理** (對齊 RU301 §4.1)
  - `redis-cli --cluster create` 命令
  - `CLUSTER SLOTS` / `CLUSTER NODES` / `CLUSTER INFO` / `CLUSTER KEYSLOT`
  - 節點新增 / 移除 / Slot 遷移
- [ ] **WL-100-03** — **redis-cli with Cluster** (對齊 RU301 §4.2)
  - `redis-cli -c` cluster mode
  - `-MOVED` / `-ASK` 重定向理解
- [ ] **WL-100-04** — **Lettuce Cluster 配置** (對齊 RU301 §4.3)
  - `LettuceConnectionFactory` Cluster 模式
  - Topology Refresh 配置
  - 自動重定向處理
- [ ] **WL-100-05** — **Hash Tag** 跨 Key 操作
  - 問題：多 Key 操作要求 Key 在同一 Slot
  - 解決：`{user:123}:cart` + `{user:123}:orders` → 相同 Hash Tag
  - MGET / Pipeline / Transaction 跨 Slot 限制
- [ ] **WL-100-06** — **Cluster Failover**
  - Master 故障 → Replica 自動升級
  - 手動 Failover: `CLUSTER FAILOVER`
  - 腦裂 (Split-Brain) 防護：`cluster-node-timeout`
- [ ] **WL-100-07** — **Cluster 容量規劃**
  - 記憶體規劃：每節點 maxmemory
  - Slot 分配策略：均衡 vs 加權
  - 電商場景：按商品類別分區的 Key 設計
- [ ] **WL-100-08** — **測試**: Cluster 資料分布驗證
  - 寫入 10K Key → 驗證 Slot 分布均勻度
  - Hash Tag 驗證：相關 Key 同 Slot
- [ ] **WL-100-09** — **測試**: Cluster Failover 測試
  - Kill Master Node → 驗證自動切換 + 資料完整性
- [ ] **WL-100-10** — **Quiz**: 10 題（80% 通過）

---

## Module 11 — RediSearch 全文檢索與索引

### WL-110: Search, Indexing & Full-Text Search

> **對齊**: Redis University RU201 — Querying, Indexing, Full-Text Search
> **學習時間**: 5-6 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | Redis Stack — RediSearch Module |
| **領域範例** | 電商：商品全文搜尋 / 金融：交易紀錄查詢 |
| **學習目標** | FT.CREATE Index, FT.SEARCH Query, Aggregation, Autocomplete |
| **依賴** | Redis Stack image (含 RediSearch module) |

#### 工作項目

- [ ] **WL-110-01** — **RediSearch 概觀**
  - 反向索引 (Inverted Index) 原理
  - 支援的索引類型：TEXT, NUMERIC, TAG, GEO, VECTOR
  - 記憶體效率：壓縮反向索引
- [ ] **WL-110-02** — **建立索引** (`FT.CREATE`)
  - 電商商品索引：

  ```
  FT.CREATE idx:products ON HASH PREFIX 1 product:
    SCHEMA
      name TEXT WEIGHT 5.0
      description TEXT
      category TAG SEPARATOR ","
      price NUMERIC SORTABLE
      brand TAG
      location GEO
  ```

  - 金融交易索引：

  ```
  FT.CREATE idx:transactions ON HASH PREFIX 1 txn:
    SCHEMA
      accountId TAG
      amount NUMERIC SORTABLE
      type TAG
      description TEXT
      createdAt NUMERIC SORTABLE
  ```

- [ ] **WL-110-03** — **查詢語法** (`FT.SEARCH`)
  - 全文搜尋：`FT.SEARCH idx:products "wireless headphones"`
  - TAG 過濾：`@category:{electronics}`
  - 數值範圍：`@price:[100 500]`
  - 組合查詢：`@category:{electronics} @price:[0 200] wireless`
  - 否定查詢、模糊搜尋、前綴搜尋
- [ ] **WL-110-04** — **Aggregation** (`FT.AGGREGATE`)
  - 電商：各類別平均價格統計
  - 金融：按月交易金額加總
  - `GROUPBY`, `REDUCE SUM`, `REDUCE AVG`, `SORTBY`
- [ ] **WL-110-05** — **Autocomplete / Suggestion** (`FT.SUGADD` / `FT.SUGGET`)
  - 電商搜尋框自動完成
  - Trie 資料結構原理
  - 加權建議（熱門搜尋權重更高）
- [ ] **WL-110-06** — **Redis OM Spring 整合**
  - `@Document` / `@Indexed` / `@Searchable` 標註
  - `RedisDocumentRepository` 自動索引建立
  - Entity Streams API：Java 8 Streams 風格查詢
  - 電商：`ProductDocument` 完整建模

  ```java
  @Document
  public class ProductDocument {
      @Id private String id;
      @Searchable(weight = 5.0) private String name;
      @Searchable private String description;
      @Indexed private Set<String> categories;
      @Indexed(sortable = true) private Double price;
      @Indexed private String brand;
      @Indexed private Point location;
  }
  ```

- [ ] **WL-110-07** — **六角形架構整合**
  - Port: `ProductSearchPort(search, autocomplete, aggregate)`
  - Adapter: `RedisSearchProductAdapter`（Redis OM Spring 實作）
  - 替代 Adapter: `ElasticsearchProductAdapter`（展示 Port 可替換性）
- [ ] **WL-110-08** — **測試**: 搜尋功能整合測試
  - 全文搜尋精確度
  - TAG 過濾正確性
  - Aggregation 統計驗證
  - Autocomplete 回傳排序
- [ ] **WL-110-09** — **測試**: 索引效能測試
  - 10K / 100K 商品的搜尋延遲
  - 索引記憶體佔用
- [ ] **WL-110-10** — **Quiz**: 12 題（80% 通過）

#### AI Prompt 指引

```
請為 module-11 實作 RediSearch 全文檢索教學。對齊 Redis University RU201。
要求：
1. 電商商品索引：TEXT + TAG + NUMERIC + GEO 欄位
2. 金融交易索引：TAG + NUMERIC 組合查詢
3. Aggregation: 類別統計、月度交易加總
4. Autocomplete: FT.SUGADD/FT.SUGGET 搜尋建議
5. Redis OM Spring: @Document + @Searchable + Entity Streams
6. 六角形架構：ProductSearchPort 可替換為 Elasticsearch
7. 使用 AbstractRedisStackIntegrationTest
8. 效能測試：10K 商品搜尋延遲 benchmark
```

---

## Module 12 — RedisJSON + Vector Search

### WL-120: JSON Documents & Vector Similarity Search

> **對齊**: Redis University — RedisJSON 課程 + Vector Search 新課程
> **學習時間**: 5-6 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | Redis Stack — RedisJSON + Vector Search |
| **領域範例** | 電商：複雜商品文件 + AI 語意搜尋 / 金融：客戶 Profile + 風險相似度 |
| **學習目標** | JSON 文件操作、JSON Path、Vector Embedding、KNN/範圍搜尋 |

#### 工作項目

- [ ] **WL-120-01** — **RedisJSON 基礎**
  - 命令：`JSON.SET`, `JSON.GET`, `JSON.DEL`, `JSON.MGET`
  - JSON Path 語法：`$.name`, `$.items[*].price`, `$.address.city`
  - 部分更新：`JSON.SET product:123 $.price 29.99`
  - 原子操作：`JSON.NUMINCRBY`, `JSON.ARRAPPEND`, `JSON.ARRPOP`
- [ ] **WL-120-02** — **複雜領域物件建模**
  - 電商：巢狀商品文件 (Product → Variants → Reviews → Ratings)

  ```json
  {
    "productId": "PROD-001",
    "name": "Wireless Headphones Pro",
    "variants": [
      {"sku": "WHP-BLK", "color": "Black", "price": 149.99, "stock": 42},
      {"sku": "WHP-WHT", "color": "White", "price": 149.99, "stock": 18}
    ],
    "reviews": {
      "averageRating": 4.5,
      "count": 1250,
      "recent": [...]
    }
  }
  ```

  - 金融：客戶 Profile (Customer → Accounts → Preferences → RiskProfile)
- [ ] **WL-120-03** — **JSON + RediSearch 組合查詢**
  - `FT.CREATE` on JSON documents
  - JSON Path 索引：`$.variants[*].price AS variant_price NUMERIC`
  - 巢狀欄位搜尋
- [ ] **WL-120-04** — **Redis OM Spring JSON Document**
  - `@Document` entity mapping to RedisJSON
  - 巢狀物件自動序列化
  - Repository 自動查詢方法
- [ ] **WL-120-05** — **Vector Similarity Search** (對齊新 Vector Search 課程)
  - 概念：Embedding → Vector → Similarity (Cosine / Euclidean / IP)
  - 建立向量索引：

  ```
  FT.CREATE idx:products ON JSON PREFIX 1 product:
    SCHEMA
      $.name AS name TEXT
      $.embedding AS embedding VECTOR FLAT 6
        TYPE FLOAT32
        DIM 768
        DISTANCE_METRIC COSINE
  ```

  - KNN 搜尋：`FT.SEARCH idx:products "*=>[KNN 5 @embedding $query_vec]"`
  - 混合搜尋：傳統過濾 + 向量相似度
- [ ] **WL-120-06** — **AI 整合場景**
  - 電商：商品語意搜尋（使用者搜「適合跑步的鞋」→ 向量比對）
  - 金融：相似風險 Profile 客戶分群
  - 模擬 Embedding 生成（Random Vector for testing）
- [ ] **WL-120-07** — **測試**: JSON CRUD 整合測試
  - 巢狀更新驗證
  - JSON Path 查詢驗證
- [ ] **WL-120-08** — **測試**: Vector Search 測試
  - KNN 結果排序驗證
  - 混合搜尋結果驗證
- [ ] **WL-120-09** — **Quiz**: 10 題（80% 通過）

#### AI Prompt 指引

```
請為 module-12 實作 RedisJSON + Vector Search 教學。對齊 Redis University 新課程。
要求：
1. RedisJSON: 電商巢狀商品文件 CRUD + 部分更新
2. JSON Path: $.variants[*].price 深層查詢
3. JSON + RediSearch: 巢狀欄位建立索引與搜尋
4. Redis OM Spring: @Document 巢狀物件映射
5. Vector Search: 建立 FLAT 向量索引 + KNN 搜尋
6. 混合搜尋：TAG 過濾 + Vector 相似度
7. AI 場景：模擬 Embedding (Random Vector) 做語意搜尋
8. 使用 AbstractRedisStackIntegrationTest
```

---

## Module 13 — 安全、監控與生產最佳實踐

### WL-130: Security, Observability & Production Best Practices

> **對齊**: Redis University — Redis Security 課程 + RU301 Chapter 5 Observability
> **學習時間**: 5-6 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | Redis Security (ACL, TLS) + RU301 Observability (Metrics, Monitoring) |
| **領域範例** | 金融：合規安全要求 / 通用：生產監控 |
| **學習目標** | ACL, TLS/mTLS, 記憶體管理, Slow Log, Metrics, 生產 Checklist |

#### 工作項目

##### 安全 (Security)

- [ ] **WL-130-01** — **Access Control Lists (ACL)** (對齊 Redis Security 課程)
  - `ACL SETUSER` / `ACL LIST` / `ACL GETUSER` / `ACL DELUSER`
  - 權限範圍：命令 / Key Pattern / Channel
  - 金融場景：Read-Only 用戶 (報表) vs Read-Write (交易)

  ```
  ACL SETUSER report_user on >secureP@ss ~banking:account:* +get +hget +hgetall -@dangerous
  ACL SETUSER txn_user on >txnP@ss ~banking:* +@all -@dangerous
  ```

- [ ] **WL-130-02** — **TLS/SSL 加密**
  - Redis TLS 配置：`tls-port`, `tls-cert-file`, `tls-key-file`, `tls-ca-cert-file`
  - Lettuce TLS 連線配置
  - 金融合規：傳輸加密要求 (PCI-DSS)
- [ ] **WL-130-03** — **安全最佳實踐**
  - 停用危險命令：`rename-command FLUSHALL ""`
  - `protected-mode` + `bind` 配置
  - 密碼策略：`requirepass` + ACL 用戶密碼
  - 網路隔離：Redis 不暴露公網

##### 記憶體管理

- [ ] **WL-130-04** — **記憶體管理與淘汰策略**
  - `maxmemory` 配置
  - 淘汰策略比較：

  | 策略 | 說明 | 適用場景 |
  |------|------|---------|
  | `noeviction` | 拒絕寫入 | 金融（不可遺失） |
  | `allkeys-lru` | LRU 淘汰 | 一般快取 |
  | `allkeys-lfu` | LFU 淘汰 | 熱點數據 |
  | `volatile-ttl` | 優先淘汰短 TTL | 混合用途 |

  - `MEMORY USAGE` / `MEMORY DOCTOR` / `MEMORY STATS`
  - Big Key 偵測：`redis-cli --bigkeys` / `MEMORY USAGE`

##### 可觀測性 (Observability, 對齊 RU301 §5)

- [ ] **WL-130-05** — **Slow Log 分析**
  - `slowlog-log-slower-than` 配置 (微秒)
  - `SLOWLOG GET` / `SLOWLOG LEN` / `SLOWLOG RESET`
  - 常見慢查詢模式辨識
- [ ] **WL-130-06** — **INFO 命令深度解讀**
  - `INFO server` / `INFO clients` / `INFO memory` / `INFO stats` / `INFO replication`
  - 關鍵指標：`used_memory`, `connected_clients`, `keyspace_hits/misses`, `instantaneous_ops_per_sec`
  - Cache Hit Rate 計算：`hits / (hits + misses)`
- [ ] **WL-130-07** — **Micrometer + Prometheus + Grafana**
  - Spring Boot Actuator + Micrometer Redis Metrics
  - 自訂指標：Cache Hit Rate, Command Latency Histogram
  - Prometheus `redis_exporter` 配置
  - Grafana Dashboard JSON 匯入
  - `docker-compose.yml` 整合 Prometheus + Grafana
- [ ] **WL-130-08** — **Latency 分析**
  - `redis-cli --latency` / `redis-cli --latency-history`
  - `LATENCY HISTORY` / `LATENCY LATEST`
  - 延遲來源辨識：網路 / Fork / Swap / Big Key
- [ ] **WL-130-09** — **連線池監控**
  - Lettuce 連線池指標
  - `CLIENT LIST` 連線分析
  - 連線洩漏偵測
- [ ] **WL-130-10** — **熱點 Key 偵測**
  - `redis-cli --hotkeys` (需 LFU 策略)
  - `OBJECT FREQ`
  - 電商秒殺場景：熱點 Key 分散策略 (Key 分片)

##### 生產 Checklist

- [ ] **WL-130-11** — **Production Readiness Checklist**

  | 類別 | 檢查項 | 狀態 |
  |------|--------|------|
  | 安全 | ACL 配置、TLS 啟用、危險命令停用 | ☐ |
  | 記憶體 | maxmemory 設定、淘汰策略選擇 | ☐ |
  | 持久化 | RDB/AOF 配置、備份排程 | ☐ |
  | 高可用 | Sentinel 或 Cluster 配置 | ☐ |
  | 監控 | Slow Log、Metrics、Alert 規則 | ☐ |
  | 連線 | 連線池配置、timeout 設定 | ☐ |
  | Key 管理 | 命名規範、TTL 策略、Big Key 掃描 | ☐ |

- [ ] **WL-130-12** — **測試**: ACL 權限驗證
- [ ] **WL-130-13** — **測試**: 效能基準測試套件 (throughput, p99)
- [ ] **WL-130-14** — **測試**: 記憶體淘汰策略驗證
- [ ] **WL-130-15** — **Quiz**: 15 題（80% 通過）

---

## Module 14 — 綜合實戰 + 認證準備

### WL-140: Capstone Project & Certification Prep

> **對齊**: Redis University 全部課程整合 + 認證準備
> **學習時間**: 10-12 小時

| 項目 | 說明 |
|------|------|
| **對應概念** | Module 01-13 全部知識整合 |
| **領域範例** | 完整金融交易系統 + 電商訂單系統 |
| **學習目標** | 端到端交付 + 架構決策文件 + 認證模擬考 |

#### 金融子系統：即時交易處理平台

- [ ] **WL-140-01** — **領域建模**
  - Aggregate: Account, Transaction, RiskAlert
  - Value Object: Money, Currency, AccountId, TransactionId
  - Domain Event: AccountOpened, MoneyDeposited, MoneyWithdrawn, RiskAlertRaised
- [ ] **WL-140-02** — **Redis 技術整合**

  | 功能 | Redis 技術 | Module 來源 |
  |------|-----------|------------|
  | 帳戶餘額快取 | String + Cache-Aside | M01, M04 |
  | 帳戶 Profile | RedisJSON | M12 |
  | 交易排行榜 | Sorted Set | M02 |
  | 防重複扣款 | Bloom Filter + Distributed Lock | M03, M08* |
  | 風控告警 | Redis Streams + Consumer Group | M07 |
  | 帳戶事件溯源 | Redis Streams (Event Store) | M07 |
  | 交易查詢 | RediSearch Index | M11 |
  | 即時匯率 | TimeSeries | M03 |
  | 監控指標 | Micrometer + Prometheus | M13 |
  | 安全 | ACL (報表/交易用戶分離) | M13 |

  *M08 = 分散式模式，整合在 Capstone 中

#### 電商子系統：訂單與庫存平台

- [ ] **WL-140-03** — **領域建模**
  - Aggregate: Order, Product, ShoppingCart, StockLevel
  - Value Object: OrderId, ProductId, CartItem, Address
  - Domain Event: OrderCreated, OrderPaid, OrderShipped, StockDeducted
- [ ] **WL-140-04** — **Redis 技術整合**

  | 功能 | Redis 技術 | Module 來源 |
  |------|-----------|------------|
  | 購物車 | Hash | M02 |
  | 庫存原子扣減 | Lua Script | M05 |
  | 商品快取 | Read-Through + Multi-Level | M04 |
  | 商品搜尋 | RediSearch + Autocomplete | M11 |
  | 商品文件 | RedisJSON (巢狀) | M12 |
  | 訂單事件 | Redis Streams | M07 |
  | API 限流 | Lua Token Bucket | M05 |
  | Session | Spring Session + Redis | — |
  | 附近門市 | Geospatial | M03 |
  | UV 統計 | HyperLogLog | M03 |
  | 語意搜尋 | Vector Search | M12 |

#### 架構與測試

- [ ] **WL-140-05** — **六角形架構完整實作**
  - Inbound Adapter: REST Controller (OpenAPI 3.1)
  - Outbound Adapter: Redis (多種技術) + H2 (模擬 DB)
  - Port 介面完整定義
  - Application Use Case 編排
- [ ] **WL-140-06** — **完整測試金字塔**

  | 層級 | 數量 (金融) | 數量 (電商) | 工具 |
  |------|-----------|-----------|------|
  | Unit Test | 20+ | 20+ | JUnit 5 + Mockito |
  | Integration Test | 15+ | 15+ | Testcontainers (Redis Stack) |
  | Contract Test | 8+ | 8+ | Port 介面行為驗證 |
  | E2E Test | 5+ | 5+ | REST API → Redis → Verify |
  | Performance Test | 3+ | 3+ | Throughput + p99 |

- [ ] **WL-140-07** — **分散式模式整合**
  - Distributed Lock (Redisson): 防重複扣款
  - Rate Limiter (Lua Token Bucket): API 限流
  - Idempotency Key (SETNX + TTL): 支付冪等
  - Global Unique ID (INCR + Timestamp): 分散式 ID 生成

#### 文件產出

- [ ] **WL-140-08** — **Architecture Decision Records (ADR)**
  - ADR-001: 選擇 Lettuce 而非 Jedis
  - ADR-002: 選擇 Cache-Aside 模式
  - ADR-003: 使用 Redis Streams 而非 Pub/Sub
  - ADR-004: Hybrid 持久化策略
  - ADR-005: Cluster vs Sentinel 部署選擇
- [ ] **WL-140-09** — **Redis Key Schema 設計規範書**
- [ ] **WL-140-10** — **部署拓撲演進圖** (Standalone → Sentinel → Cluster)
- [ ] **WL-140-11** — **效能測試報告**

#### 認證模擬考

- [ ] **WL-140-12** — **模擬考試** (80 題, 80% 通過)
  - 對齊 Redis 認證考試七大領域：

  | 領域 | 題數 | 涵蓋模組 |
  |------|------|---------|
  | General Redis Knowledge | 10 | M01 |
  | Keys & Expiration | 10 | M01-02 |
  | Data Structures | 15 | M02-03 |
  | Data Modeling | 10 | M06, M11-12 |
  | Debugging & Troubleshooting | 10 | M13 |
  | Performance Optimization | 15 | M04-05, M13 |
  | Clustering & HA | 10 | M08-10 |

---

## 學習路徑建議

### 路徑 A: 快速入門（2 週，每日 2 小時）

```
Week 1: M01 → M02 → M04 → M05
Week 2: M07 → M08 → M13(部分) → Mini Capstone
```

### 路徑 B: 完整學習（6 週，每日 2 小時）

```
Week 1: M01 → M02
Week 2: M03 → M04
Week 3: M05 → M06
Week 4: M07 → M08
Week 5: M09 → M10 → M11
Week 6: M12 → M13 → M14
```

### 路徑 C: 對齊 Redis University 認證（4 週密集）

```
Week 1: M01 + M02 + M03 (= RU101 Get Started with Redis)
Week 2: M04 + M05 + M06 (= RU102J Redis for Java Developers)
Week 3: M07 + M08 + M09 + M10 (= RU301 Running Redis at Scale + Streams)
Week 4: M11 + M12 + M13 + M14 (= RU201 + Security + Capstone)
```

---

## 模組依賴關係圖

```
M01 (入門連線)
 │
 ├─ M02 (核心結構) ──── M03 (進階結構) ──────────────────────┐
 │                                                           │
 ├─ M04 (快取模式) ──── M05 (Pipeline/Tx/Lua)                │
 │                       │                                   │
 │                       └─ M06 (資料建模) ──┐               │
 │                                           │               │
 ├─ M07 (Streams/事件) ─────────────────────┼───────────────┤
 │                                           │               │
 ├─ M08 (持久化) ───── M09 (高可用) ── M10 (Cluster) ──────┤
 │                                                           │
 ├─ M11 (RediSearch) ── M12 (JSON/Vector) ─────────────────┤
 │                                                           │
 └─ M13 (安全/監控) ───────────────────────────────────────┤
                                                             │
                                                        M14 (Capstone)
```

---

## 測試策略總覽

### 測試分層規範

| 層級 | 範圍 | 工具 | Redis Image | 執行時機 |
|------|------|------|-------------|----------|
| Unit Test | Domain Service, VO | JUnit 5 + Mockito | 無 | 每次 commit |
| Integration Test | Redis Adapter | Testcontainers | `redis:7-alpine` | 每次 PR |
| Stack Integration | Search/JSON/TS/Bloom | Testcontainers | `redis/redis-stack:7.4` | 每次 PR |
| Contract Test | Port 介面行為 | JUnit 5 | 無 | 每次 PR |
| Performance Test | Throughput, Latency | JMH / Custom | `redis:7-alpine` | Release 前 |
| Chaos Test | Failover, Recovery | Testcontainers lifecycle | `redis:7-alpine` | Release 前 |

### 測試命名慣例

```
{Method}_{Scenario}_{ExpectedResult}

範例：
- getBalance_WhenAccountExists_ReturnsBalance
- deductStock_WhenInsufficientQuantity_ThrowsInsufficientStockException
- acquireLock_WhenAlreadyLockedByAnother_ReturnsFalse
- pipeline_WhenBatch1000Commands_CompletesWithin100ms
- searchProducts_WhenQueryMatchesMultiple_ReturnsSortedByRelevance
- vectorSearch_WhenKNN5_ReturnsTop5MostSimilar
```

---

## 交付檢查清單 (Definition of Done)

每個 Sub Module 完成時，須通過以下檢查：

- [ ] 所有程式碼遵循六角形架構 Package 慣例
- [ ] Domain Layer 無任何框架依賴（純 Java POJO）
- [ ] Port 介面定義完整（Inbound + Outbound）
- [ ] Adapter 實作正確注入 Port（DIP）
- [ ] 單元測試覆蓋 Domain Service 核心邏輯（≥ 80%）
- [ ] 整合測試使用 Testcontainers（基礎 Redis 或 Redis Stack）
- [ ] 測試命名符合 `{Method}_{Scenario}_{ExpectedResult}` 慣例
- [ ] 無硬編碼連線資訊（皆透過 application.yml + DynamicPropertySource）
- [ ] Key 命名符合 `{service}:{entity}:{id}` 慣例
- [ ] 包含 Quiz 測驗（≥ 80% 通過）
- [ ] README.md 含模組說明、對齊 Redis University 課程、學習重點
- [ ] AI Prompt 指引可獨立重現模組內容

---

## 版本與相容性

| 元件 | 版本 | 備註 |
|------|------|------|
| Java | 23 | Virtual Threads 可選啟用 |
| Spring Boot | 4.x | spring-boot-starter-data-redis |
| Gradle | 8.x+ | Kotlin DSL |
| Redis Stack | 7.4+ | 含 RediSearch 2.10, RedisJSON 2.8, RedisTimeSeries 1.12, RedisBloom 2.8 |
| Lettuce | 6.x | 預設 Redis Client |
| Jedis | 7.x | Redis Stack Module 命令 (輔助) |
| Redis OM Spring | 1.x | RedisJSON + RediSearch ORM |
| Testcontainers | 1.19+ | Redis + Redis Stack Module |
| Redisson | 3.x | Module 14 Distributed Lock |
| Micrometer | 1.12+ | Module 13 Observability |

---

> **使用方式**: 將此工作清單作為 AI 助理的上下文，逐模組執行開發。
> 每次開發一個 Sub Module 時，提供該模組的「工作項目」與「AI Prompt 指引」給 AI，
> 即可生成完整的程式碼、測試與文件。
>
> **參考連結**:
> - [Redis University](https://university.redis.io/) — 官方免費課程
> - [Redis OM Spring](https://github.com/redis/redis-om-spring) — Spring Data 擴展
> - [Redis Stack](https://redis.io/about/about-stack/) — 模組化平台
> - [Running Redis at Scale](https://redis.io/tutorials/operate/redis-at-scale/) — 線上教材
