# 00 · 動手前：需求分析與設計決策

> 大部分 Redis 事故的根因不在 Redis，在「沒人問過這個系統到底需要什麼」——快取當資料庫用、
> 沒定 RPO 就選了 `everysec`、資料量估錯十倍才發現要 Cluster。這一章在你敲第一個指令之前，
> 先把需求問清楚，再把每個需求對應到一個設計決策，並留下決策記錄。

## 目錄

1. [為什麼要先做需求分析](#1-為什麼要先做需求分析)
2. [需求訪談：十二個問題](#2-需求訪談十二個問題)
3. [需求 → 設計決策對照表](#3-需求--設計決策對照表)
4. [設計決策逐項說明](#4-設計決策逐項說明)
5. [範例：電商的商品快取 + 購物車 + Session](#5-範例電商的商品快取--購物車--session)
6. [設計決策記錄（ADR）範本](#6-設計決策記錄adr範本)
7. [設計評審檢查清單](#7-設計評審檢查清單)

---

## 1. 為什麼要先做需求分析

**不做會怎樣**：Redis 的每個設定都是取捨——`appendfsync always` 安全但慢 30 倍、`allkeys-lru` 省心但會丟資料、Cluster 能擴充但多 key 指令受限。沒有需求，就沒有取捨的依據，只能抄別人的設定；抄來的設定在別人的需求下是對的，在你的可能是災難。

**怎麼做**：把「需求」拆成能量化的問題（第 2 節），每個答案都指向一個設計決策（第 3、4 節），決策寫成一頁 ADR（第 6 節）留給半年後的自己和接手的人。整個過程通常一小時，比事故後補救便宜得多。

**先講清楚 Redis 的定位**：它是「加速層」還是「主要儲存」？這一個問題決定後面一半的答案。
- **加速層**（快取、Session、排行榜、計數）：資料丟了能從別處重建 → 可以用淘汰策略、可以關持久化、單機就能上線
- **主要儲存**（佇列裡未處理的訂單、唯一的計數器、分散式鎖狀態）：丟了就是事故 → 要持久化、要複寫、要定 RPO、要 `noeviction` + 監控

同一個 Redis 混用兩種定位是最常見的設計錯誤：快取把記憶體吃滿，淘汰策略把訂單佇列淘汰掉。**混用的解法是分實例**，不是調參數。

---

## 2. 需求訪談：十二個問題

每個問題都附「為什麼要問」與「答案影響什麼」。答不出來的問題，就用保守假設並寫進 ADR 的「假設」欄。

| # | 問題 | 為什麼要問 | 答案影響的決策 |
|---|---|---|---|
| 1 | **這些資料丟了會怎樣？能從別處重建嗎？** | 決定 Redis 是加速層還是主要儲存 | 持久化、淘汰策略、拓撲、要不要分實例 |
| 2 | **可以接受丟幾秒 / 幾分鐘的資料？（RPO）** | `everysec` 丟 1 秒、非同步複寫丟數秒、每日備份丟一天 | `appendfsync`、`min-replicas-to-write`、備份頻率 |
| 3 | **服務中斷多久會被罵？（RTO）** | 人工切換要 5–30 分鐘、Sentinel 秒級、單機重啟 = 載入 RDB 的時間 | 單機 vs Sentinel vs Cluster、資料集大小上限 |
| 4 | **資料量多大？一年後呢？** | 超過單機記憶體（實務上 30–50 GB）就要分片；fork 時間與資料量成正比 | 機型、`maxmemory`、單機 vs Cluster、分實例 |
| 5 | **讀寫 QPS 各多少？尖峰是平均的幾倍？** | 單機單執行緒約 10–15 萬 ops/s（無 pipeline）；寫入無法靠 Replica 分攤 | Pipeline、讀寫分離、Cluster、io-threads |
| 6 | **延遲目標是多少？p99 還是平均？** | 決定能不能跨可用區、能不能開 TLS、能不能接受 fork 尖峰 | 部署位置、持久化方式、大 key 上限 |
| 7 | **存取模式：key 有多大、一個 key 多少元素、有沒有多 key 操作？** | 大 key 與熱 key 是效能事故主因；多 key 操作在 Cluster 受限 | Key 設計、資料結構選擇、hash tag、能不能用 Cluster |
| 8 | **資料的生命週期：誰產生、誰刪、多久過期？** | 沒有 TTL 的快取就是記憶體洩漏 | TTL 策略、淘汰策略、`maxmemory` |
| 9 | **一致性要求：讀到舊資料可以嗎？可以舊多久？** | Replica 是非同步的；快取與資料庫之間永遠有窗口 | 讀寫分離要不要、快取更新策略（先刪後更新 / 雙刪）、`WAIT` |
| 10 | **誰會連進來？跨網路嗎？有合規要求嗎？** | 無密碼綁公網幾分鐘就被入侵；金融 / 醫療通常要求傳輸加密與稽核 | bind、ACL、TLS、`ACL LOG`、防火牆 |
| 11 | **預算與團隊：幾台機器？誰維運？有沒有人懂 Sentinel / Cluster？** | Cluster 的維運複雜度是單機的數倍；沒人值班的 Sentinel 等於沒有 | 拓撲、託管服務 vs 自建、監控告警的深度 |
| 12 | **要不要用模組（JSON / Search / TimeSeries / Bloom）？** | 決定映像檔 / 套件來源（[03 §2](03-deploy-vm.md#2-三條安裝路線)）；Search 有自己的記憶體開銷 | 安裝路線、容量估算 |

---

## 3. 需求 → 設計決策對照表

**怎麼用這張表**：拿第 2 節的答案，逐列找到對應的決策；每個決策在第 4 節有理由與反例。

| 需求答案 | 設計決策 | 章節 |
|---|---|---|
| 資料能重建（Q1） | 加速層：`allkeys-lru` / `allkeys-lfu`、可關持久化、單機或主從 | [01 §4](01-architecture.md#4-過期與淘汰記憶體滿了怎麼辦) |
| 資料不能重建（Q1） | 主要儲存：`noeviction` + 記憶體告警、AOF `everysec` 以上、至少主從、獨立實例 | [01 §5](01-architecture.md#5-持久化架構rdbaof混合) |
| RPO ≤ 1 秒（Q2） | AOF `everysec` + 主從 + `min-replicas-to-write 1`；RPO = 0 幾乎做不到，改由業務端冪等補償 | [06 §5](06-operations.md#5-備份與還原) |
| RPO = 一天可接受（Q2） | RDB + 每日備份異地 | [06 §5](06-operations.md#5-備份與還原) |
| RTO 分鐘級、有人值班（Q3） | 主從 + 手動切換 runbook | [06 §6](06-operations.md#6-日常操作) |
| RTO 秒級、無人值班（Q3） | Sentinel（3 個、不同機器）或 Cluster | [01 §6](01-architecture.md#6-四種拓撲單機主從sentinelcluster) |
| 資料 < 單機記憶體 60%（Q4） | 單機 / 主從 / Sentinel；`maxmemory` = 實體 60–70% | [01 §7](01-architecture.md#7-容量規劃) |
| 資料 > 單機或一年內會超過（Q4） | Cluster，或依業務切多個獨立實例（更簡單） | [01 §6](01-architecture.md#6-四種拓撲單機主從sentinelcluster) |
| 寫入 QPS > 10 萬（Q5） | Cluster（寫入只能靠分片擴充）+ Pipeline | [04 §2](04-performance-tuning.md#2-網路往返pipeline批次指令連線池) |
| 讀多寫少、讀 QPS 高（Q5） | 主從讀寫分離 + 應用端本地快取 | [04 §4](04-performance-tuning.md#4-大-key-與熱-key) |
| p99 < 1 ms（Q6） | 同可用區、不開 TLS 或接受 +10–20%、大 key 上限 10 KB、關 THP | [04](04-performance-tuning.md) |
| 有多 key 交易 / Lua 跨 key（Q7） | 避免 Cluster，或用 hash tag 讓相關 key 同 slot | [01 §6](01-architecture.md#6-四種拓撲單機主從sentinelcluster) |
| 集合會超過 5000 元素（Q7） | 一開始就按 id 拆桶；用 Stream 取代大 List | [04 §4](04-performance-tuning.md#4-大-key-與熱-key) |
| 資料有明確過期（Q8） | 寫入即帶 TTL + 隨機抖動；`volatile-*` 策略 | [05 §1](05-use-cases.md#1-快取cache-aside) |
| 讀到舊資料不行（Q9） | 讀寫都走 Master；快取更新用「先更新 DB 再刪快取」+ 延遲雙刪；關鍵寫入用 `WAIT` | [05 §1](05-use-cases.md#1-快取cache-aside) |
| 跨網路 / 有合規（Q10） | TLS、ACL 每個應用一個帳號、`ACL LOG` 稽核、bind 內網 | [06 §1–3](06-operations.md#1-安全五道防線) |
| 團隊小、沒人值班（Q11） | 單機 + 每日備份 + 明確的 RTO 承諾，或用雲端託管；不要硬上 Cluster | — |
| 需要 JSON / Search（Q12） | `redis:8` 映像或 apt 套件；原始碼路線不含模組 | [03 §2](03-deploy-vm.md#2-三條安裝路線) |

---

## 4. 設計決策逐項說明

每項：**要決定什麼 → 為什麼重要 → 怎麼決定 → 常見錯誤**。

### 4.1 定位：加速層還是主要儲存

要決定：這個實例裡的資料丟了算不算事故。
為什麼重要：它決定淘汰策略（能不能丟）、持久化（要不要留）、拓撲（要不要自動切換）——三個最貴的決策。
怎麼決定：問 Q1。答案是「看情況」就代表混用了，**拆成兩個實例**。
常見錯誤：一個 Redis 同時放商品快取（能丟）和訂單佇列（不能丟），`allkeys-lru` 把佇列淘汰掉。

### 4.2 拓撲：單機 / 主從 / Sentinel / Cluster

要決定：幾台、什麼角色、掛了誰接。
為什麼重要：拓撲決定 RTO 與容量上限，事後改拓撲要停機或複雜遷移。
怎麼決定：[01 §6](01-architecture.md#6-四種拓撲單機主從sentinelcluster) 的決策樹——先問資料量與寫入 QPS 是否超過單機（→ Cluster），再問能不能接受人工切換（→ Sentinel）。
常見錯誤：「以後可能會大」就上 Cluster，結果多 key 操作到處 `CROSSSLOT`、維運沒人會；或反過來單機撐到 80 GB，fork 要 5 秒。
折衷：**依業務拆成多個獨立的主從 / Sentinel 實例**通常比 Cluster 簡單，且天然隔離故障。

### 4.3 持久化：RDB / AOF / 混合 / 關閉

要決定：`save`、`appendonly`、`appendfsync`。
為什麼重要：直接等於 RPO；也影響延遲（fork、fsync）。
怎麼決定：Q1 能重建 → 可關（重啟後暖機的資料庫壓力要評估）；不能 → 混合持久化 + `everysec`；RPO 要更小 → 主從 + `min-replicas-to-write`，而不是 `always`。
常見錯誤：以為 `everysec` 是「不會丟」；或在 Replica 上做持久化、Master 關掉，卻讓 Master 設成自動重啟——Master 空著起來會把 Replica 也清空。

### 4.4 記憶體：maxmemory 與淘汰策略

要決定：`maxmemory`、`maxmemory-policy`、機型。
為什麼重要：不設 `maxmemory` = 等 OOM killer；策略錯 = 丟不該丟的或拒絕該收的。
怎麼決定：[01 §7](01-architecture.md#7-容量規劃) 的公式估算 → 塞樣本資料實測 `used_memory` → × 1.2 碎片 → `maxmemory` ≤ 實體 60–70%（留 fork 空間）→ 策略依 4.1 定位。
常見錯誤：只算 value 大小忘了每個 key 約 60 bytes 包裝，估少 3–5 倍；`maxmemory` 設成實體記憶體 100%。

### 4.5 Key 與資料結構設計

要決定：命名慣例、每種資料用什麼結構、每個 key 的上限與 TTL。
為什麼重要：這是唯一「上線後幾乎改不了」的決策——改 key 格式等於資料遷移。大 key 與熱 key 都在這裡埋下。
怎麼決定：[01 §8](01-architecture.md#8-key-與資料建模原則) 的慣例 + [05 選型速查表](05-use-cases.md#選型速查表)；每個 key 回答四個問題：**誰寫、誰讀、多大、多久過期**。
常見錯誤：一個 List 當全域佇列無限長；Hash 存整個使用者的所有歷史；快取 key 沒 TTL。

### 4.6 Client 端策略

要決定：連線池大小、逾時、重試、拓撲感知（Sentinel / Cluster client）、本地快取。
為什麼重要：Redis 端 7 秒完成切換，應用若不刷新拓撲會一直打舊 Master；逾時太長會讓一個慢 Redis 拖垮整個服務。
怎麼決定：逾時 100–500 ms + 有限重試；Sentinel / Cluster 一定用對應的 client 模式；熱 key 場景加本地快取。
常見錯誤：每個請求開新連線；逾時用預設的無限；用普通 client 連 Sentinel 環境。

### 4.7 安全

要決定：bind、密碼 / ACL、TLS、危險指令。
為什麼重要：無密碼綁公網幾分鐘內被入侵，且 Redis 有能力寫任意檔案（`CONFIG SET dir` + `SAVE`）。
怎麼決定：[06 §1](06-operations.md#1-安全五道防線) 五道防線全部做；Q10 有跨網路或合規 → TLS + `ACL LOG`。
常見錯誤：「內網不用密碼」——內網被打穿的第一站就是沒密碼的 Redis。

### 4.8 可觀測性與運維

要決定：監控哪些指標、告警門檻、備份頻率與異地、誰值班、演練頻率。
為什麼重要：沒有告警的 Sentinel 掛了兩個沒人知道（S9）；沒演練過的備份等於沒備份。
怎麼決定：[04 §8](04-performance-tuning.md#8-監控指標與告警門檻) 的門檻表、[06 §5](06-operations.md#5-備份與還原) 的備份與演練、[07](07-troubleshooting.md) 的情境先演練一遍。
常見錯誤：只監控「活著」；備份放在同一台機器。

---

## 5. 範例：電商的商品快取 + 購物車 + Session

**需求訪談結果**：

| 問題 | 答案 |
|---|---|
| Q1 丟了會怎樣 | 商品快取：能從 DB 重建。購物車：**不能**（使用者放的東西不見會客訴）。Session：能重新登入但體驗差 |
| Q2 RPO | 購物車 ≤ 1 秒；其他不在乎 |
| Q3 RTO | 大促期間 < 30 秒，平時 5 分鐘可接受 |
| Q4 資料量 | 商品 50 萬件 × 2 KB = 1 GB；購物車 200 萬活躍 × 500 B = 1 GB；Session 500 萬 × 300 B = 1.5 GB；一年成長 2 倍 |
| Q5 QPS | 讀 8 萬 / 寫 5 千，大促 5 倍 → 讀 40 萬 |
| Q6 延遲 | API p99 < 50 ms，Redis 這段 < 2 ms |
| Q7 存取模式 | 商品：單 key 讀；購物車：Hash 單 key 讀寫；Session：Hash 單 key。**沒有多 key 交易** |
| Q8 生命週期 | 商品快取 10 分鐘；購物車 30 天；Session 30 分鐘滑動 |
| Q9 一致性 | 商品價格改了 10 秒內要看到；購物車要即時 |
| Q10 安全 | 內網；有稽核要求（誰改了購物車） |
| Q11 團隊 | 3 人後端、沒有 DBA、有雲端 |
| Q12 模組 | 不需要 |

**推導出的設計決策**：

| 決策 | 內容 | 從哪個需求來 |
|---|---|---|
| 分實例 | **兩個 Redis**：`cache`（商品 + Session，加速層）與 `cart`（購物車，主要儲存） | Q1：定位不同不能混用（4.1） |
| `cache` 實例 | 主從 + Sentinel；`maxmemory 6gb` `allkeys-lru`；`save ""`、`appendonly no`；重啟後由 DB 暖機（有限流） | Q1 能重建、Q3 大促 RTO 30 秒、Q4 2.5 GB × 2 倍成長 |
| `cart` 實例 | 主從 + Sentinel；`maxmemory 4gb` `noeviction` + 80% 告警；AOF `everysec` 混合模式；`min-replicas-to-write 1` | Q1 不能丟、Q2 RPO 1 秒 |
| 不用 Cluster | 總量 3.5 GB、成長 2 倍仍遠低於單機；寫入 5 千；沒人維運 Cluster | Q4、Q5、Q11、Q7 沒有多 key 需求也不需要 hash tag |
| 讀擴充 | 大促 40 萬讀：`cache` 的兩個 Replica 分攤 + 應用端 Caffeine 本地快取 5 秒（熱門商品） | Q5 尖峰、Q6 延遲、[04 §4](04-performance-tuning.md#4-大-key-與熱-key) 熱 key |
| Key 設計 | `product:{sku}`（String JSON, EX 600 + 抖動）；`cart:{userId}`（Hash, EXPIRE 30d 每次更新續期）；`session:{sid}`（Hash, EXPIRE 1800 滑動） | Q7、Q8；購物車上限 500 項（超過拒絕）避免大 key |
| 一致性 | 價格更新：先更新 DB → `DEL product:{sku}` → 延遲 1 秒再 `DEL` 一次；購物車只走 Master | Q9 |
| 安全 | bind 內網；ACL：`app-cache`（`~product:* ~session:* +@read +@write -@dangerous`）、`app-cart`（`~cart:*`）、`monitoring`；`ACL LOG` 接稽核 | Q10 |
| 運維 | Grafana 儀表板 + 告警（記憶體 80%、`connected_slaves`、bgsave 狀態、CKQUORUM）；`cart` 每日 RDB 備份到物件儲存、每季還原演練；上線前用 `scenario.sh` 演練 S1/S4/S9 | Q11、4.8 |
| 機型 | 兩個實例各三台（1 主 2 從）8 GB RAM；Sentinel 與 Redis 同機但三台分散在三個可用區 | Q4 × 1.5 fork 空間、Q3 |

**這個範例告訴你的事**：需求只回答了 12 個問題，就自然推出「兩個實例、不用 Cluster、哪個開 AOF」——這些如果用猜的，最常見的結果是一個實例、`allkeys-lru`、購物車在大促時被淘汰。

---

## 6. 設計決策記錄（ADR）範本

**為什麼要寫**：半年後沒有人記得為什麼 `cart` 是 `noeviction`，新人「優化」成 `allkeys-lru`，大促當天出事。ADR 一頁就夠，重點是**當時的需求與被否決的選項**。

```markdown
# ADR-003：購物車使用獨立 Redis 實例並開啟 AOF

## 狀態
已採納（2026-08-29）

## 背景（需求）
- 購物車資料不能從其他地方重建（Q1）；RPO ≤ 1 秒（Q2）
- 200 萬活躍使用者 × 500 B ≈ 1 GB，一年成長 2 倍（Q4）
- 同一個 Redis 目前也放商品快取（allkeys-lru）

## 決策
- 購物車搬到獨立實例 `cart`：主從 + 3 Sentinel
- maxmemory 4gb + noeviction + 80% 告警
- appendonly yes、appendfsync everysec、aof-use-rdb-preamble yes
- min-replicas-to-write 1、min-replicas-max-lag 10

## 被否決的選項
- 留在同一實例改 volatile-lru：購物車 key 也有 TTL（30 天），仍可能被淘汰
- appendfsync always：吞吐量掉 30 倍，且 Master 單機故障仍會丟；改用 min-replicas-to-write 達到接近的保證
- Cluster：資料量與寫入 QPS 都遠低於單機上限，維運成本不值得

## 後果
- 多一組機器（3 台 8 GB）
- 記憶體達 80% 時寫入會在 100% 時被拒（OOM）：必須有告警與擴容流程（見 runbook）
- 應用需改用 cart 專用的連線與 ACL 帳號

## 假設（需要驗證）
- 每個購物車 ≤ 500 項；超過會被應用拒絕（避免大 key）
- 大促寫入尖峰 ≤ 2.5 萬 QPS（單機可承受）

## 驗證方式
- scripts/verify-sentinel.sh 通過；scenario.sh S1 演練確認告警觸發
```

---

## 7. 設計評審檢查清單

上線前拿著設計文件逐條問，任何一條答不出來就回到第 2 節：

- [ ] 每個實例的定位（加速層 / 主要儲存）寫清楚了，沒有混用 —— 否則淘汰策略會丟掉不該丟的
- [ ] RPO / RTO 有數字，且持久化與拓撲能達到它 —— 否則事故時才發現承諾做不到
- [ ] 資料量估算有實測（塞樣本看 `used_memory`）、含一年成長、`maxmemory` ≤ 實體 70% —— 否則 fork 失敗或 OOM
- [ ] 寫入 QPS 與尖峰倍數已知，單機能不能撐有依據 —— 否則大促當天才知道天花板
- [ ] 每個 key 類型回答了「誰寫、誰讀、多大、多久過期」，沒有無上限的集合 —— 否則大 key 與記憶體洩漏
- [ ] 多 key 操作清單已列出，若用 Cluster 已設計 hash tag —— 否則上線後 `CROSSSLOT`
- [ ] 一致性窗口寫明（可以舊幾秒）且業務方同意 —— 否則客訴「我剛改的沒生效」
- [ ] Client 逾時、重試、拓撲感知已設定 —— 否則 Redis 切換了應用還在打舊 Master
- [ ] bind 內網、ACL 每應用一帳號、危險指令收掉、跨網路有 TLS —— 否則第一個被入侵的就是它
- [ ] 告警門檻、備份與還原演練、值班人有名字 —— 否則 Sentinel 掛了沒人知道
- [ ] 每個非顯然的決策有 ADR，含被否決的選項 —— 否則半年後被「優化」回去
- [ ] 用 `scripts/scenario.sh` 至少演練過 S1、S4、S9（記憶體、TTL、Sentinel quorum）—— 否則第一次排查在正式環境
