# Redis 實戰教學專案

> 從零到一，14 個模組帶你掌握 Redis 全棧技能 — 涵蓋資料結構、快取模式、串流處理、搜尋引擎、高可用架構到認證考試準備。
>
> 程式碼（Java / Spring Boot）之外，還有一套**給初學者的中文手冊**與**在真實環境驗證過的佈署腳本**：
> 架構設計、容器與 VM 兩種佈署、效能最佳化、應用場景、維運安全——每支腳本與設定都實際跑過，驗證方式見 [驗證方式與實測紀錄](#驗證方式與實測紀錄)。

## 手冊導覽（docs/）

| 章節 | 內容 | 適合 |
|---|---|---|
| [00 需求分析與設計決策](docs/00-requirements-and-design.md) | **動手前**：12 個需求訪談問題、需求 → 設計決策對照表、逐項決策的理由與常見錯誤、電商完整範例、ADR 範本、設計評審檢查清單 | 架任何東西之前 |
| [01 架構設計](docs/01-architecture.md) | 單執行緒事件迴圈、記憶體模型與編碼、過期淘汰、持久化、**單機 / 主從 / Sentinel / Cluster 選型決策樹**、容量規劃、反模式 | 開始寫程式前 |
| [02 容器佈署](docs/02-deploy-container.md) | 三套 Docker Compose 環境（單機 + 監控、Sentinel、Cluster）、容器特有的坑（volume、記憶體限制、核心參數、DNS 與固定 IP）、驗證 | Docker 使用者 |
| [03 VM 佈署](docs/03-deploy-vm.md) | **一鍵腳本**（apt / dnf / 原始碼編譯自動選擇）、手動 runbook、systemd、OS 調校、多台 VM 主從與 Sentinel、Vagrant、實測紀錄 | 正式環境 |
| [04 效能最佳化](docs/04-performance-tuning.md) | benchmark 實測數據、Pipeline、慢指令、大 key / 熱 key、記憶體最佳化、持久化對延遲的影響、監控門檻、檢查清單 | 上線前後 |
| [05 應用場景](docs/05-use-cases.md) | 16 個模式用 `redis-cli` 走一遍：快取（穿透 / 擊穿 / 雪崩）、Session、計數、排行榜、分散式鎖、限流、佇列、Pub/Sub、Geo、Bitmap、布隆、冪等、延遲佇列… | 想知道怎麼用 |
| [06 維運與安全](docs/06-operations.md) | ACL、TLS、監控告警、備份還原、日常操作、升級、**錯誤訊息對照表**、上線檢查清單 | 維運 |
| [07 問題排查](docs/07-troubleshooting.md) | 排查心法與固定順序、第一分鐘五個指令、**12 個可親手模擬的故障情境**（OOM、大 key、連線耗盡、無 TTL、MISCONF、FLUSHALL、熱 key、AOF 損毀、Sentinel 不切換、CLUSTERDOWN、NOPERM、Replica 斷線）各附排查順序、根因、修復、預防 | 出事時與出事前演練 |

每一章的每個重點都依「**為什麼**（不做會發生什麼）→ **怎麼做** → **具體指令 / 設定**」的順序寫，先有前提再有做法。

**建議路線**：初學者 → 01 → 02（把單機環境跑起來）→ 05（邊看邊敲指令）→ Module 01–06 程式碼 → 04 → 03 / 06 → 07（用 `scripts/scenario.sh` 把每個情境弄壞再修好）。要設計正式環境時從 00 開始。

## 技術棧

| 元件 | 版本 | 說明 |
|------|------|------|
| Java | 25 (LTS) | 最新長期支援版 |
| Spring Boot | 4.0.2 | Spring Data Redis + Web + Actuator |
| Redis | 8（實測 8.10.1） | 內建 Search、JSON、TimeSeries、Bloom、Vector Set 模組 |
| Gradle | 8.x | Kotlin DSL + Version Catalog |
| Testcontainers | 2.0.3 | 整合測試自動啟動 Redis 容器 |
| Docker Compose | v2+（實測 v5.4） | Redis + RedisInsight + redis_exporter + Prometheus + Grafana |

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

## Redis 安裝與環境準備

### 方式一：Docker（推薦）

使用 Docker 是最快速的方式，無需手動安裝 Redis，本專案也以此為主。

```bash
# 啟動基本 Redis 8（含全部模組）
docker run -d --name redis -p 6379:6379 redis:8

# 啟動輕量版 Redis（僅基本功能，約 30MB）
docker run -d --name redis -p 6379:6379 redis:8-alpine

# 驗證 Redis 是否正常運行
docker exec -it redis redis-cli ping
# 回傳 PONG 表示正常
```

> **Docker Image 選擇指南**：
> - `redis:8` — 完整版，包含 RediSearch、RedisJSON、Bloom Filter、TimeSeries 模組
> - `redis:8-alpine` — 精簡版，僅含核心功能（String、List、Set、Hash、Sorted Set 等），體積小

本專案提供現成的 Docker Compose 配置，一鍵啟動完整開發環境：

```bash
# 啟動 Redis + RedisInsight + Prometheus + Grafana
docker compose up -d

# 查看服務狀態
docker compose ps

# 停止服務
docker compose down
```

### 方式二：macOS 安裝

```bash
# 使用 Homebrew 安裝
brew install redis

# 啟動 Redis（前台執行，方便觀察日誌）
redis-server

# 或以背景服務方式啟動
brew services start redis

# 停止背景服務
brew services stop redis

# 驗證
redis-cli ping
```

### 方式三：Linux VM 安裝（Ubuntu / Debian / RHEL / Fedora…）

本專案提供一鍵佈署腳本，會自動選擇路線（Ubuntu / Debian 走 `packages.redis.io` 官方倉庫；其他發行版原始碼編譯），
並完成 systemd 服務、OS 調校與驗證：

```bash
sudo deploy/vm/install-redis.sh --password 'your-password'     # 全部自動；DRY_RUN=1 先看會做什麼
sudo scripts/verify-vm.sh -a 'your-password'                    # 19 項檢查（systemd、權限、OS 參數、重啟不丟資料）
```

手動安裝的逐步 runbook、每個設定的理由、多台 VM 組主從 / Sentinel、Vagrant 練習環境，見 [docs/03-deploy-vm.md](docs/03-deploy-vm.md)。

### 方式四：Windows 安裝

Windows 使用者建議透過 WSL2（Windows Subsystem for Linux）安裝：

```powershell
# 1. 啟用 WSL2（以系統管理員身分執行 PowerShell）
wsl --install

# 2. 重新啟動電腦後，進入 WSL 的 Ubuntu 環境
wsl

# 3. 在 WSL 中按照 Linux 步驟安裝 Redis
```

或直接使用 Docker Desktop for Windows，搭配本專案的 Docker Compose 配置。

### 基本配置說明

Redis 的主要配置檔為 `redis.conf`，以下是常用配置項目：

```conf
# 綁定位址（預設只允許本機連線）
bind 127.0.0.1

# 連接埠
port 6379

# 密碼設定（正式環境必須設定）
requirepass your-password

# 最大記憶體限制
maxmemory 256mb

# 記憶體淘汰策略（記憶體不足時如何處理）
maxmemory-policy allkeys-lru

# 持久化 — RDB 快照
save 900 1       # 900 秒內至少 1 次寫入時做快照
save 300 10      # 300 秒內至少 10 次寫入
save 60 10000    # 60 秒內至少 10000 次寫入

# 持久化 — AOF 日誌
appendonly yes
appendfsync everysec
```

本專案的 Docker 配置檔位於 `docker/redis/redis.conf`，可依需求自行調整。

### 管理工具：RedisInsight

本專案的 Docker Compose 已整合 [RedisInsight](https://redis.io/insight/)，是 Redis 官方推出的 GUI 管理工具：

```bash
docker compose up -d
# 啟動後瀏覽器開啟 http://localhost:5540
```

RedisInsight 提供：
- **視覺化瀏覽** — 以樹狀結構瀏覽所有 Key
- **指令行** — 內建 CLI，支援語法提示與自動完成
- **效能分析** — 即時查看 SLOWLOG 與記憶體使用狀況
- **Profiler** — 監控即時指令流量

---

## Redis 基本操作概念

### 連線 Redis

```bash
# 連線本機 Redis
redis-cli

# 連線遠端 Redis
redis-cli -h <host> -p <port>

# 帶密碼連線
redis-cli -h <host> -p <port> -a <password>

# 連線 Docker 容器中的 Redis
docker exec -it redis redis-cli
```

### Key-Value 基礎模型

Redis 是一個 **Key-Value 資料庫**，所有資料都以 Key 為索引。Key 是字串，Value 則可以是多種資料型別。

```
┌──────────────────────────────────────────────────────┐
│                    Redis Server                       │
│                                                      │
│   Key（字串）           Value（多種型別）               │
│   ─────────────────    ──────────────────────         │
│   "user:1001:name"  →  "Alice"          (String)     │
│   "user:1001:score" →  95               (String)     │
│   "cart:1001"       →  {item1:2,item2:1} (Hash)      │
│   "recent:views"    →  [p3, p1, p5]      (List)      │
│   "tags:post:42"    →  {redis,nosql,db}  (Set)       │
│   "leaderboard"     →  [(Alice,95),(Bob,87)] (ZSet)  │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### Key 命名慣例

良好的 Key 命名對維護性至關重要，本專案採用 `service:entity:id` 格式：

```
✅ 推薦格式
  user:1001:name          → 用戶 1001 的名稱
  product:sku-123:cache   → 商品 SKU-123 的快取
  order:2024:stream       → 2024 年度訂單串流
  cart:user:1001          → 用戶 1001 的購物車

❌ 避免的格式
  u1001                   → 太短，難以理解
  user_name_1001          → 不符合 Redis 慣例（應用冒號分隔）
  myapp:data              → 太籠統，無法區分資料內容
```

### 核心資料型別速覽

| 型別 | 說明 | 常用指令 | 典型場景 |
|------|------|---------|---------|
| **String** | 最基本的型別，可存字串、數字、JSON | `SET` `GET` `INCR` `DECR` | 快取、計數器、Session |
| **List** | 有序的字串鏈結串列 | `LPUSH` `RPUSH` `LRANGE` `BLPOP` | 訊息佇列、最近瀏覽 |
| **Set** | 無序且不重複的字串集合 | `SADD` `SMEMBERS` `SINTER` | 標籤、共同好友 |
| **Hash** | 欄位-值對的集合（類似物件） | `HSET` `HGET` `HGETALL` | 物件存取、購物車 |
| **Sorted Set** | 帶分數的有序集合 | `ZADD` `ZRANGE` `ZRANK` | 排行榜、優先佇列 |
| **Stream** | 追加式的日誌資料結構 | `XADD` `XREAD` `XREADGROUP` | 事件驅動、訊息串流 |
| **HyperLogLog** | 基數估計（不重複計數） | `PFADD` `PFCOUNT` | UV 統計 |
| **Geo** | 地理空間索引 | `GEOADD` `GEOSEARCH` | 附近搜尋 |
| **Bitmap** | 位元操作 | `SETBIT` `GETBIT` `BITCOUNT` | 簽到、活躍追蹤 |

### 基本 CRUD 操作示範

```bash
# === String 型別 ===

# 設定值
SET user:1001:name "Alice"

# 取得值
GET user:1001:name
# "Alice"

# 設定值並指定過期時間（60 秒後自動刪除）
SET session:abc123 "user-data" EX 60

# 數值遞增（原子操作，適合計數器）
SET page:views 0
INCR page:views       # 1
INCR page:views       # 2
INCRBY page:views 10  # 12

# === Key 管理 ===

# 查看 Key 是否存在
EXISTS user:1001:name  # 1（存在）

# 設定過期時間（秒）
EXPIRE user:1001:name 3600  # 1 小時後過期

# 查看剩餘存活時間
TTL user:1001:name     # 剩餘秒數，-1 表示永不過期，-2 表示已過期

# 刪除 Key
DEL user:1001:name

# 查詢符合模式的 Key（僅限開發環境，正式環境應使用 SCAN）
KEYS user:*

# === Hash 型別 ===

# 設定 Hash 欄位
HSET cart:1001 item-a 2 item-b 1

# 取得單一欄位
HGET cart:1001 item-a
# "2"

# 取得所有欄位與值
HGETALL cart:1001
# "item-a" "2" "item-b" "1"

# 欄位值遞增
HINCRBY cart:1001 item-a 3  # item-a 數量變為 5

# === List 型別 ===

# 從左側推入
LPUSH recent:user:1001 "product-5" "product-3" "product-1"

# 取得範圍（0 到 -1 表示全部）
LRANGE recent:user:1001 0 -1
# "product-1" "product-3" "product-5"

# === Sorted Set 型別 ===

# 新增成員與分數
ZADD leaderboard 95 "Alice" 87 "Bob" 92 "Charlie"

# 按分數從高到低取前 3 名（附帶分數）
ZREVRANGE leaderboard 0 2 WITHSCORES
# "Alice" "95" "Charlie" "92" "Bob" "87"
```

### TTL（Time To Live）過期機制

Redis 支援為 Key 設定存活時間，過期後自動刪除，是實現快取的關鍵機制：

```bash
# 設定時同時指定 TTL
SET cache:product:1001 "{...}" EX 300   # 300 秒（5 分鐘）
SET cache:session:abc  "{...}" PX 30000 # 30000 毫秒（30 秒）

# 對已存在的 Key 設定 TTL
EXPIRE cache:product:1001 600  # 重設為 600 秒
PEXPIRE cache:product:1001 600000  # 毫秒精度

# 查看剩餘時間
TTL cache:product:1001    # 回傳秒數
PTTL cache:product:1001   # 回傳毫秒數

# 移除 TTL（變為永不過期）
PERSIST cache:product:1001
```

> **注意**：TTL 的設計直接影響快取命中率與記憶體使用量。太短會頻繁回源（cache miss），太長可能導致資料過時。Module 04 會深入探討各種 TTL 策略。

### 原子性與單執行緒模型

Redis 使用**單執行緒**處理所有指令，這代表：

1. **每個指令都是原子的** — `INCR`、`LPUSH`、`SETNX` 等操作不需要額外的鎖機制
2. **無競態條件** — 不會出現兩個指令同時修改同一個 Key 的情況
3. **指令依序執行** — 所有 Client 的指令排隊依序處理

```
Client A: INCR counter  ──┐
Client B: INCR counter  ──┼──→ [Redis 指令佇列] ──→ 依序執行
Client C: GET counter   ──┘
```

> **為什麼單執行緒還這麼快？** Redis 將資料存放在記憶體中，避免了磁碟 I/O 的瓶頸。加上使用 I/O 多路複用（epoll / kqueue）處理網路連線，單執行緒即可達到每秒十萬級別的吞吐量。

---

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
│   ├── sentinel/                 # Sentinel 設定（固定 IP，見 docs/02）
│   ├── cluster/
│   ├── prometheus/
│   └── grafana/                  # 資料來源 + 自動載入的 Redis 儀表板
├── docs/                         # 中文手冊：00 需求與設計 01 架構 02 容器 03 VM 04 效能 05 場景 06 維運 07 排查
├── deploy/vm/                    # VM 一鍵佈署：install-redis.sh、redis.conf 範本、systemd unit、sysctl、Vagrantfile
├── scripts/                      # 驗證與維運腳本（見下方「驗證方式與實測紀錄」）
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

### 基礎篇（Module 01-03）— 約 6.5 小時

| 模組 | 主題 | Redis 技術 | 測試數 | 學習時間 |
|------|------|-----------|--------|---------|
| [Module 01](#module-01-getting-started) | Redis 入門 | String（SET/GET/INCR/TTL） | 15 | 1.5 hr |
| [Module 02](#module-02-data-structures) | 核心資料結構 | List / Set / Hash / Sorted Set | 18 | 2 hr |
| [Module 03](#module-03-specialized-structures) | 特殊資料結構 | HyperLogLog / Geo / Bitmap / Bloom Filter / TimeSeries | 40 | 3 hr |

### 進階篇（Module 04-06）— 約 6 小時

| 模組 | 主題 | Redis 技術 | 測試數 | 學習時間 |
|------|------|-----------|--------|---------|
| [Module 04](#module-04-caching-patterns) | 快取模式 | Cache-Aside / Read-Through / TTL 策略 | 15 | 2 hr |
| [Module 05](#module-05-pipelining-transactions) | Pipeline 與交易 | Pipeline / MULTI-EXEC / Lua Script | 17 | 2 hr |
| [Module 06](#module-06-data-modeling) | 資料建模 | 嵌入式 vs 引用式 / 反正規化 / Key 設計 | 15 | 2 hr |

### 應用篇（Module 07-10）— 約 9.5 小時

| 模組 | 主題 | Redis 技術 | 測試數 | 學習時間 |
|------|------|-----------|--------|---------|
| [Module 07](#module-07-streams-events) | 串流與事件驅動 | Streams / Consumer Group / XADD / XREADGROUP | 23 | 2.5 hr |
| [Module 08](#module-08-persistence) | 持久化 | RDB / AOF / 混合持久化 / RPO-RTO 分析 | 19 | 2 hr |
| [Module 09](#module-09-high-availability) | 高可用（Sentinel） | Sentinel / 自動故障轉移 / 讀寫分離 | 20 | 2.5 hr |
| [Module 10](#module-10-clustering) | 叢集 | Cluster / Hash Slot（CRC16）/ 拓撲規劃 | 21 | 2.5 hr |

### 專家篇（Module 11-13）— 約 5.5 小時

| 模組 | 主題 | Redis 技術 | 測試數 | 學習時間 |
|------|------|-----------|--------|---------|
| [Module 11](#module-11-search-indexing) | 全文搜尋與索引 | RediSearch（FT.CREATE / FT.SEARCH / FT.AGGREGATE） | 15 | 2 hr |
| [Module 12](#module-12-json-vector-search) | JSON 與向量搜尋 | RedisJSON（JSON.SET/GET）/ Vector Search（KNN） | 15 | 2 hr |
| [Module 13](#module-13-security-production) | 安全與正式環境 | ACL / SLOWLOG / INFO / 淘汰策略 / 上線檢查清單 | 10 | 1.5 hr |

### 總整合（Module 14）— 約 4 小時

| 模組 | 主題 | Redis 技術 | 測試數 | 學習時間 |
|------|------|-----------|--------|---------|
| [Module 14](#module-14-capstone) | Capstone 總整合 | **整合以上全部** + 分散式鎖 / 冪等性 / 限流 | 57 | 4 hr |

> **全專案共 444 個測試，涵蓋單元測試、整合測試與 Quiz 測驗。預估總學習時間約 31.5 小時。**
>
> 學習時間包含閱讀程式碼、執行測試、動手練習 `redis-cli` 指令與完成 Quiz 測驗。實際時間依個人經驗而異，有 Redis 基礎者可適當加速。

---

## 各模組詳細說明

### Module 01: Getting Started

> **預估學習時間：1.5 小時** — 包含環境設定、基本指令練習與 Quiz 測驗

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

> **預估學習時間：2 小時** — 四種資料結構各需 30 分鐘實作與練習

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

> **預估學習時間：3 小時** — 五種特殊結構 + Lua 腳本整合，內容較豐富

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

> **預估學習時間：2 小時** — 理解快取模式的差異與適用場景是關鍵

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

> **預估學習時間：2 小時** — Pipeline 與 Lua 腳本需要動手實驗才能深入理解

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

> **預估學習時間：2 小時** — 建模思維需要搭配實際案例反覆推敲

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

> **預估學習時間：2.5 小時** — Consumer Group 機制較複雜，建議搭配 `redis-cli` 逐步操作

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

> **預估學習時間：2 小時** — 重點在理解 RDB 與 AOF 的取捨及 RPO/RTO 計算

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

> **預估學習時間：2.5 小時** — 建議啟動 Docker Sentinel 環境實際觀察故障轉移

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

> **預估學習時間：2.5 小時** — Hash Slot 與叢集拓撲規劃需要時間消化

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

> **預估學習時間：2 小時** — 索引設計與查詢語法是核心，建議多練習 FT.SEARCH 查詢

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

> **預估學習時間：2 小時** — JSONPath 語法與向量搜尋概念各需約 1 小時

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

> **預估學習時間：1.5 小時** — 偏重觀念與檢查清單，適合快速掌握

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

> **預估學習時間：4 小時** — 整合所有模組知識，含金融與電商兩大子系統 + 80 題認證模擬考

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

三套環境互相獨立（各有 Compose `name:`），細節與踩坑紀錄見 [docs/02-deploy-container.md](docs/02-deploy-container.md)。

### 基本環境

```bash
docker compose up -d                        # 全部
docker compose up -d redis redis-insight    # 只要 Redis + GUI
./scripts/verify-single.sh                  # 驗證
```

| 服務 | 埠號 | 說明 |
|------|------|------|
| Redis 8 | 6379 | 主要 Redis 實例（含全部模組） |
| RedisInsight | 5540 | Web 管理介面 |
| redis_exporter | 9121 | Prometheus 指標 |
| Prometheus | 9090（`PROMETHEUS_PORT` 可改） | 指標收集 |
| Grafana | 3000（`GRAFANA_PORT` 可改） | 儀表板（帳密 admin/admin，自動載入「Redis Tutorial」） |

### Sentinel 環境

```bash
docker compose -f docker-compose-sentinel.yml up -d
./scripts/verify-sentinel.sh                # 含停掉 Master → 自動故障轉移 → 舊 Master 回歸的完整測試
```

| 服務 | 埠號 | 固定 IP |
|------|------|------|
| Master | 6379 | 172.28.0.10 |
| Replica 1 / 2 | 6380 / 6381 | 172.28.0.11 / .12 |
| Sentinel 1–3 | 26379–26381 | 172.28.0.21–.23 |

> 為什麼固定 IP：Master 容器停掉後 Docker DNS 解析不到名稱，Sentinel 會進入 TILT 模式而不切換——實測踩到，已修正。

### Cluster 環境

```bash
docker compose -f docker-compose-cluster.yml up -d      # redis-cluster-init 自動建立 3 主 3 從
./scripts/verify-cluster.sh
docker exec -it redis-node-1 redis-cli -c -p 7001       # 在容器內操作（-c 自動跟隨 MOVED）
```

| 服務 | 埠號 | 固定 IP |
|------|------|------|
| Node 1–6 | 7001–7006（bus 17001–17006） | 172.29.0.11–.16 |

### 一次驗證三套

```bash
./scripts/verify-all.sh        # 依序啟動 → 驗證 → 關閉；需要上述埠號空著
```

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

### `docker compose up` 說 port is already allocated

宿主機已有東西占用 6379 / 9090 / 3000（另一套 Compose、VM 安裝的 Redis、其他監控）。
`ss -tlnp | grep 6379` 找出來停掉；Prometheus / Grafana 可用 `PROMETHEUS_PORT=19090 GRAFANA_PORT=13000 docker compose up -d` 改埠。
基本環境與 Sentinel 環境都用 6379，不要同時啟動。

### `./gradlew` 報 `Could not find or load main class org.gradle.wrapper.GradleWrapperMain`

`gradle/wrapper/gradle-wrapper.jar` 沒進版控（`.gitignore` 的 `*.jar` 規則曾蓋掉例外）。已修正並提交 jar；若仍遇到：
`curl -fsSL -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/v8.14.0/gradle/wrapper/gradle-wrapper.jar`。

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

## 驗證方式與實測紀錄

本專案的每一支腳本、每一份設定檔都在下列環境**實際執行過**，任何人 clone 下來都能用同樣的指令重現。

### 驗證環境

| | 環境 |
|---|---|
| 宿主機 / VM | WSL2（Hyper-V）上的 Fedora Linux 44、Linux 6.18、16 vCPU / 15 GB、systemd 259 |
| 容器 | Docker Engine 29.6、Docker Compose v5.4、`redis:8` = 8.10.1 |
| Java | JDK 25（Gradle toolchain 自動下載）、Gradle 8.14、Testcontainers |
| 乾淨 Ubuntu | `ubuntu:24.04` 容器（驗證 apt 路線） |
| 日期 | 2026-08-28 |

### 怎麼驗證

| 對象 | 指令 | 檢查什麼 |
|---|---|---|
| Java 專案 | `./gradlew test` | 14 個模組 444 個測試（Testcontainers 自動起 Redis） |
| 單機容器環境 | `docker compose up -d && ./scripts/verify-single.sh` | 容器 healthy、`redis.conf` 每項設定生效、模組已載入、**重啟後資料仍在**、RedisInsight / exporter / Prometheus / Grafana 儀表板 |
| Sentinel 環境 | `docker compose -f docker-compose-sentinel.yml up -d && ./scripts/verify-sentinel.sh` | 拓撲、複寫、**停掉 Master → 自動切換 → 新 Master 有資料可寫 → 舊 Master 回歸變 Replica 並同步** |
| Cluster 環境 | `docker compose -f docker-compose-cluster.yml up -d && ./scripts/verify-cluster.sh` | 16384 slot、MOVED / CROSSSLOT / hash tag、**停掉一個 Master → Replica 接手 → 資料仍在 → 回歸變 Replica** |
| 三套一起 | `./scripts/verify-all.sh` | 上面三項，依序啟動 / 驗證 / 關閉 |
| 故障情境模擬 | `./scripts/scenario.sh inject N` / `reset N`；自動驗證 `./scripts/verify-scenarios.sh` | 12 個情境各驗三件事：症狀真的出現、排查指令看得到根因、還原後恢復（docs/07） |
| VM 佈署 | `sudo deploy/vm/install-redis.sh …` 後 `sudo scripts/verify-vm.sh -a 密碼` | systemd（Type=notify、redis 帳號、LimitNOFILE）、設定檔權限、OS 參數（overcommit / THP / somaxconn）、日誌無 WARNING、**`systemctl restart` 後資料仍在** |
| 資料型別與應用場景 | `./scripts/smoke-test.sh [-a 密碼]` | 9 種資料型別 + 交易 / Lua + 分散式鎖 + 限流，28 項斷言 |
| 健康檢查 | `./scripts/health-check.sh` | 記憶體、持久化、連線、slowlog、複寫；exit code 可接監控 |
| 效能 | `./scripts/benchmark.sh` | 基準 / Pipeline / value 大小 / 延遲分佈 |
| 備份 | `./scripts/backup.sh -d 目錄` | BGSAVE → 複製 → `redis-check-rdb` |

### 實測結果

| 項目 | 結果 |
|---|---|
| `./gradlew test` | **444 / 444 通過**（M01 40、M02 73、M03 50、M04 39、M05 26、M06 29、M07 23、M08 19、M09 20、M10 21、M11 15、M12 15、M13 17、M14 57） |
| `verify-all.sh` | **全部通過**：單機 11 / 11、Sentinel 18 / 18、Cluster 14 / 14 |
| `verify-scenarios.sh` | **42 / 42 通過**：12 個故障情境注入 → 症狀斷言 → 還原斷言（單機 9 個、Sentinel 2 個、Cluster 1 個） |
| Sentinel 故障轉移 | 停掉 Master 後 **7 秒** `+switch-master`（quorum 3/2、leader 選舉 → 升級 Replica → 重新設定其他 Replica） |
| Cluster 故障轉移 | 停掉一個 Master 後 **9 秒** Replica 升級、`cluster_state:ok`、資料可讀 |
| 監控堆疊 | Prometheus `redis_up=1`、Grafana 自動載入「Redis Tutorial」14 個面板 |
| VM（Fedora，原始碼編譯） | `install-redis.sh` 7 步全綠，編譯 31 秒；`verify-vm.sh` **19 / 19**；`systemctl restart` 後 **214 ms** 恢復服務 |
| VM（Ubuntu 24.04，apt） | `packages.redis.io` 安裝 8.10.1，JSON / Search / Bloom / TimeSeries 模組自動 `loadmodule`；`smoke-test.sh` 28 / 28 |
| `smoke-test.sh` | 28 / 28 |
| `benchmark.sh`（VM） | 無 pipeline：SET 140k / GET 151k ops/s；Pipeline 16：SET 952k / GET 1.43M；100 KB value：SET 5.7k；p50 0.079 ms |

### 這次驗證抓到並修正的問題

| 問題 | 症狀 | 修正 |
|---|---|---|
| Sentinel 環境起不來 | 三個 Sentinel 啟動即崩潰：`Can't resolve instance hostname` | `sentinel.conf` 改用固定 IP；Compose 指派 `172.28.0.0/24` |
| Sentinel 不做故障轉移 | Master 停掉後 Sentinel 進入 `+tilt`（DNS 阻塞） | 同上（用 IP 就不查 DNS） |
| 三個 Sentinel 共用一份 bind-mount 設定檔 | 互相覆蓋、檔案被改成 root 擁有 | `entrypoint` 先複製到容器內再啟動 |
| Cluster 停掉一個 Master 後整個叢集 `fail` | `cluster-announce-ip` 用容器名稱，節點消失後 DNS 阻塞主執行緒，Replica 無法升級 | 改固定 IP `172.29.0.0/24` |
| `./gradlew` 無法執行 | `gradle-wrapper.jar` 被 `.gitignore` 的 `*.jar` 排除 | 調整規則順序並提交 jar |
| 主 Compose 的 `prometheus` / `grafana` 容器名稱 | 與同機其他專案衝突 | 改名 `redis-prometheus` / `redis-grafana`，埠號可用環境變數覆寫 |
| Redis 8.10 原始碼 `make` 連模組一起編 | 需要 LLVM 21 + Rust，一般 VM 沒有 | 腳本改 `make build redis` 只編核心 |

## 學習建議

1. **依序學習** — 從 Module 01 開始，每個模組建立在前一個的基礎上
2. **先讀測試** — 測試是最好的文件，先看測試理解預期行為
3. **動手實作** — 啟動 Docker 環境，用 `redis-cli` 手動練習指令
4. **理解架構** — 觀察六角形架構如何將業務邏輯與 Redis 操作分離
5. **挑戰 Quiz** — 完成每個模組的 Quiz 測驗，確認理論知識
6. **認證準備** — Module 14 的 80 題模擬考對應 Redis 認證七大領域

## 授權

本專案僅供教學與學習用途。
