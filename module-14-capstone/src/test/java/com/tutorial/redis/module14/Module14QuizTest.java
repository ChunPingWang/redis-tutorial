package com.tutorial.redis.module14;

import com.tutorial.redis.common.quiz.Quiz;
import com.tutorial.redis.common.quiz.QuizQuestion;
import com.tutorial.redis.common.quiz.QuizResult;
import com.tutorial.redis.common.quiz.QuizRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Module 14 Redis 認證模擬考試 — 80 題")
class Module14QuizTest {

    private Quiz createCertificationExam() {
        List<QuizQuestion> questions = new ArrayList<>();

        // =====================================================================
        // Domain 1: General Redis Knowledge (Q1-Q10)
        // =====================================================================

        questions.add(new QuizQuestion(1,
                "Redis 是什麼類型的資料庫？",
                List.of("關聯式資料庫", "記憶體內鍵值資料庫", "文件資料庫", "圖形資料庫"),
                1,
                "Redis 是一個開源的記憶體內鍵值資料庫，支援多種資料結構。"));

        questions.add(new QuizQuestion(2,
                "Redis 使用什麼執行緒模型來處理命令？",
                List.of("多執行緒並行處理", "單執行緒事件迴圈", "執行緒池", "Actor 模型"),
                1,
                "Redis 使用單執行緒事件迴圈處理所有命令，避免了鎖競爭，保證了操作的原子性。"));

        questions.add(new QuizQuestion(3,
                "Redis 使用的通訊協定名稱是？",
                List.of("HTTP", "gRPC", "RESP (Redis Serialization Protocol)", "AMQP"),
                2,
                "Redis 使用 RESP（Redis Serialization Protocol）作為客戶端與伺服器之間的通訊協定。"));

        questions.add(new QuizQuestion(4,
                "以下哪個不是 Redis 原生支援的資料類型？",
                List.of("String", "List", "關聯式表格（Table）", "Sorted Set"),
                2,
                "Redis 不支援關聯式表格。原生支援的資料類型包括 String、List、Hash、Set、Sorted Set 等。"));

        questions.add(new QuizQuestion(5,
                "Redis 與 Memcached 的主要區別之一是？",
                List.of("Redis 不支援持久化", "Redis 支援多種資料結構而 Memcached 只支援字串",
                        "Memcached 效能遠優於 Redis", "Memcached 支援更多資料類型"),
                1,
                "Redis 支援豐富的資料結構（String、List、Hash、Set、Sorted Set 等），而 Memcached 主要支援簡單的鍵值字串。"));

        questions.add(new QuizQuestion(6,
                "redis-cli 中執行 PING 命令，正常情況下伺服器回應什麼？",
                List.of("OK", "PONG", "1", "CONNECTED"),
                1,
                "Redis 伺服器收到 PING 命令後正常回應 PONG，用於測試連線是否存活。"));

        questions.add(new QuizQuestion(7,
                "CONFIG GET maxmemory 命令的作用是？",
                List.of("設定最大記憶體", "取得目前最大記憶體配置值", "重啟 Redis 伺服器", "清除記憶體"),
                1,
                "CONFIG GET 用於在運行時取得 Redis 配置參數的當前值，maxmemory 是記憶體上限設定。"));

        questions.add(new QuizQuestion(8,
                "Redis 8 引入的重大改變之一是？",
                List.of("移除所有資料結構", "統一授權模型，將模組功能整合進核心",
                        "不再支援持久化", "改用多執行緒處理命令"),
                1,
                "Redis 8 將許多原本作為模組的功能（如 RediSearch、RedisJSON 等）整合進核心，統一了授權模型。"));

        questions.add(new QuizQuestion(9,
                "Redis 預設監聽的 TCP 端口是？",
                List.of("3306", "5432", "6379", "27017"),
                2,
                "Redis 預設監聽 TCP 端口 6379。3306 是 MySQL，5432 是 PostgreSQL，27017 是 MongoDB。"));

        questions.add(new QuizQuestion(10,
                "關於 Redis 的持久化機制，以下哪個敘述正確？",
                List.of("Redis 只支援 RDB 持久化", "Redis 支援 RDB 和 AOF 兩種持久化機制",
                        "Redis 不支援任何持久化", "Redis 只支援 AOF 持久化"),
                1,
                "Redis 支援 RDB（快照）和 AOF（追加檔案）兩種持久化機制，也可同時啟用形成混合持久化。"));

        // =====================================================================
        // Domain 2: Keys & Expiration (Q11-Q20)
        // =====================================================================

        questions.add(new QuizQuestion(11,
                "以下哪個是 Redis Key 命名的最佳實踐？",
                List.of("使用冒號分隔的層次結構，如 user:1001:profile",
                        "使用空格分隔，如 user 1001 profile",
                        "使用盡可能長的 Key 名稱",
                        "Key 名稱不區分大小寫"),
                0,
                "Redis 社群慣例使用冒號（:）作為 Key 的層次分隔符，如 user:1001:profile。"));

        questions.add(new QuizQuestion(12,
                "TTL 命令回傳 -1 代表什麼意思？",
                List.of("Key 不存在", "Key 存在但未設定過期時間", "Key 已過期", "TTL 為 1 秒"),
                1,
                "TTL 回傳 -1 表示 Key 存在但沒有設定過期時間；回傳 -2 表示 Key 不存在。"));

        questions.add(new QuizQuestion(13,
                "PTTL 與 TTL 命令的區別是？",
                List.of("PTTL 回傳毫秒精度，TTL 回傳秒精度",
                        "PTTL 是永久保留，TTL 是臨時保留",
                        "兩者完全相同",
                        "PTTL 只能用於 String 類型"),
                0,
                "PTTL 以毫秒為單位回傳剩餘生存時間，TTL 以秒為單位回傳。兩者功能相同但精度不同。"));

        questions.add(new QuizQuestion(14,
                "PERSIST 命令的作用是？",
                List.of("刪除 Key", "移除 Key 的過期時間，使其永久保留",
                        "設定 Key 的過期時間", "持久化 Key 到磁碟"),
                1,
                "PERSIST 命令移除 Key 上已設定的過期時間，使該 Key 變為永久保留（不會自動過期）。"));

        questions.add(new QuizQuestion(15,
                "在生產環境中，為什麼應該使用 SCAN 而不是 KEYS 命令？",
                List.of("SCAN 速度更快", "KEYS 會阻塞伺服器直到掃描完成，SCAN 是增量式迭代",
                        "KEYS 已被棄用", "SCAN 支援更多資料類型"),
                1,
                "KEYS 命令會一次掃描整個 keyspace 並阻塞伺服器，對於大型資料庫可能導致嚴重延遲。SCAN 使用游標進行增量迭代，不會長時間阻塞。"));

        questions.add(new QuizQuestion(16,
                "TYPE 命令的作用是？",
                List.of("回傳 Key 儲存的值的資料類型", "變更 Key 的資料類型",
                        "建立特定類型的 Key", "刪除特定類型的 Key"),
                0,
                "TYPE 命令回傳指定 Key 儲存的值的資料類型，如 string、list、set、zset、hash、stream 等。"));

        questions.add(new QuizQuestion(17,
                "RENAME 命令在目標 Key 已存在時會怎樣？",
                List.of("回傳錯誤", "覆蓋目標 Key", "忽略操作", "將兩個 Key 合併"),
                1,
                "RENAME 命令會將來源 Key 重新命名為目標 Key。如果目標 Key 已存在，它會被覆蓋。使用 RENAMENX 可以在目標不存在時才重新命名。"));

        questions.add(new QuizQuestion(18,
                "DEL 和 UNLINK 命令的區別是？",
                List.of("DEL 是同步刪除（阻塞），UNLINK 是非同步刪除（非阻塞）",
                        "兩者完全相同",
                        "UNLINK 只能刪除 String 類型",
                        "DEL 不會真正刪除資料"),
                0,
                "DEL 同步刪除 Key 並釋放記憶體（可能阻塞），UNLINK 先將 Key 從 keyspace 移除，然後在背景執行緒中非同步釋放記憶體。"));

        questions.add(new QuizQuestion(19,
                "OBJECT ENCODING 命令的作用是？",
                List.of("回傳 Key 的內部編碼方式", "變更 Key 的編碼",
                        "將 Key 編碼為 Base64", "壓縮 Key 的值"),
                0,
                "OBJECT ENCODING 回傳 Key 值的內部編碼方式，如 int、embstr、raw、ziplist、listpack、hashtable 等。有助於了解記憶體使用效率。"));

        questions.add(new QuizQuestion(20,
                "EXISTS 命令可以同時檢查多個 Key 嗎？回傳值代表什麼？",
                List.of("可以，回傳存在的 Key 數量", "不可以，只能檢查一個",
                        "可以，回傳 true/false", "可以，回傳不存在的 Key 數量"),
                0,
                "EXISTS 可以接受多個 Key 作為參數，回傳值是這些 Key 中存在的數量。例如 EXISTS k1 k2 k3 如果 k1 和 k3 存在則回傳 2。"));

        // =====================================================================
        // Domain 3: Data Structures (Q21-Q35)
        // =====================================================================

        questions.add(new QuizQuestion(21,
                "SET 命令的 NX 選項的作用是？",
                List.of("只在 Key 不存在時設定值", "只在 Key 存在時設定值",
                        "設定值並回傳舊值", "設定值但不觸發事件"),
                0,
                "SET key value NX 只在 Key 不存在時才設定值，常用於實現分散式鎖。類似於 SETNX 命令。"));

        questions.add(new QuizQuestion(22,
                "INCR 命令對不存在的 Key 執行時會怎樣？",
                List.of("回傳錯誤", "將 Key 初始化為 0 然後遞增為 1",
                        "忽略操作", "建立 Key 並設值為 -1"),
                1,
                "INCR 對不存在的 Key 執行時，會先將其值初始化為 0，然後遞增 1，最終 Key 的值為 1。"));

        questions.add(new QuizQuestion(23,
                "MSET 命令的特性是？",
                List.of("原子性地設定多個鍵值對", "設定單個 Key 的多個值",
                        "設定 Key 的多個過期時間", "非原子性地批量設定"),
                0,
                "MSET 原子性地同時設定多個鍵值對。所有的設定操作要麼全部完成，要麼全部不執行（原子性）。"));

        questions.add(new QuizQuestion(24,
                "LPUSH 和 RPUSH 的區別是？",
                List.of("LPUSH 從左端（頭部）插入，RPUSH 從右端（尾部）插入",
                        "LPUSH 只能插入一個，RPUSH 可以插入多個",
                        "兩者完全相同",
                        "LPUSH 用於 List，RPUSH 用於 Set"),
                0,
                "LPUSH 將元素插入 List 的左端（頭部），RPUSH 將元素插入右端（尾部）。兩者都可以一次插入多個元素。"));

        questions.add(new QuizQuestion(25,
                "BLPOP 命令的特殊之處是？",
                List.of("它是阻塞式操作，會等待直到 List 中有元素或超時",
                        "它比 LPOP 更快",
                        "它可以彈出多個元素",
                        "它只能在 Cluster 模式下使用"),
                0,
                "BLPOP 是 LPOP 的阻塞版本。如果 List 為空，客戶端會被阻塞直到有元素可以彈出或達到超時時間，常用於實現訊息佇列。"));

        questions.add(new QuizQuestion(26,
                "HSET 命令可以同時設定多個欄位嗎？",
                List.of("可以，HSET key field1 value1 field2 value2",
                        "不可以，必須使用 HMSET",
                        "可以，但最多 10 個",
                        "不可以，只能設定一個欄位"),
                0,
                "從 Redis 4.0 起，HSET 支援同時設定多個欄位值對，語法為 HSET key field1 value1 field2 value2。HMSET 已被視為過時。"));

        questions.add(new QuizQuestion(27,
                "HINCRBY 命令用於？",
                List.of("將 Hash 中指定欄位的值遞增指定的整數",
                        "遞增 Hash 的大小",
                        "新增一個欄位到 Hash",
                        "遞增 Hash 的 TTL"),
                0,
                "HINCRBY key field increment 將 Hash 中指定欄位的整數值遞增 increment。如果欄位不存在，先初始化為 0 再遞增。"));

        questions.add(new QuizQuestion(28,
                "SINTER 命令的作用是？",
                List.of("回傳多個 Set 的交集", "回傳多個 Set 的聯集",
                        "回傳多個 Set 的差集", "合併多個 Set"),
                0,
                "SINTER 回傳所有給定 Set 的交集，即同時存在於所有 Set 中的成員。SUNION 是聯集，SDIFF 是差集。"));

        questions.add(new QuizQuestion(29,
                "Sorted Set 中 ZADD 的 GT 選項的作用是？",
                List.of("只在新分數大於目前分數時才更新", "只新增不更新",
                        "只在新分數小於目前分數時才更新", "總是更新"),
                0,
                "ZADD 的 GT 選項表示只在新提供的分數大於成員目前的分數時才更新。常用於排行榜中只記錄最高分。"));

        questions.add(new QuizQuestion(30,
                "ZRANGEBYSCORE 命令的作用是？",
                List.of("回傳分數在指定範圍內的成員", "回傳排名在指定範圍內的成員",
                        "回傳按字典序排列的成員", "刪除分數在指定範圍內的成員"),
                0,
                "ZRANGEBYSCORE 回傳 Sorted Set 中分數在 min 和 max 範圍內的成員，按分數從低到高排序。"));

        questions.add(new QuizQuestion(31,
                "HyperLogLog 的主要用途是？",
                List.of("精確計算集合大小", "以極低記憶體近似估算基數（不重複元素數量）",
                        "儲存大量字串", "實現分散式鎖"),
                1,
                "HyperLogLog 使用約 12KB 記憶體即可近似估算高達 2^64 個不重複元素的基數，標準誤差約 0.81%。"));

        questions.add(new QuizQuestion(32,
                "GEOADD 命令儲存地理位置時，底層使用什麼資料結構？",
                List.of("Hash", "Sorted Set", "List", "Stream"),
                1,
                "GEOADD 底層使用 Sorted Set 儲存地理位置，將經緯度編碼為 Geohash 分數，因此可以使用 Sorted Set 的所有命令。"));

        questions.add(new QuizQuestion(33,
                "Redis Stream 中 XADD 命令的 * 作為 ID 參數代表什麼？",
                List.of("由 Redis 自動生成唯一 ID（毫秒時間戳-序號）",
                        "匹配所有 ID",
                        "使用最大 ID",
                        "使用隨機 ID"),
                0,
                "XADD 中的 * 表示讓 Redis 自動生成 Stream 條目 ID，格式為 <毫秒時間戳>-<序號>，保證單調遞增。"));

        questions.add(new QuizQuestion(34,
                "Bloom Filter 的 BF.EXISTS 命令回傳 1 代表什麼？",
                List.of("元素一定存在", "元素可能存在（有誤判率）",
                        "元素一定不存在", "過濾器已滿"),
                1,
                "Bloom Filter 的 BF.EXISTS 回傳 1 表示元素「可能存在」（有一定的假陽性率）。回傳 0 則表示元素「一定不存在」（無假陰性）。"));

        questions.add(new QuizQuestion(35,
                "RedisJSON 中 JSON.SET 命令使用什麼路徑語法存取巢狀欄位？",
                List.of("JSONPath 語法（如 $.user.name）", "XPath 語法",
                        "SQL 語法", "正規表達式"),
                0,
                "RedisJSON 使用 JSONPath 語法存取 JSON 文件中的巢狀欄位，根路徑為 $，如 $.user.name 存取 user 物件的 name 欄位。"));

        // =====================================================================
        // Domain 4: Data Modeling (Q36-Q45)
        // =====================================================================

        questions.add(new QuizQuestion(36,
                "在 Redis 中，「嵌入（Embedding）」模式與「引用（Referencing）」模式的區別是？",
                List.of("嵌入將相關資料存在同一個 Key 中，引用使用多個 Key 透過 ID 關聯",
                        "兩者完全相同",
                        "嵌入只能用於 String，引用只能用於 Hash",
                        "引用比嵌入更節省記憶體"),
                0,
                "嵌入模式將所有相關資料存在同一個結構中（如一個 Hash 或 JSON），減少查詢次數。引用模式將資料分散在不同 Key 中，透過 ID 關聯，增加靈活性。"));

        questions.add(new QuizQuestion(37,
                "Redis 中反正規化（Denormalization）的主要目的是？",
                List.of("減少記憶體使用", "以資料冗餘換取讀取效能",
                        "確保資料一致性", "簡化寫入操作"),
                1,
                "反正規化是故意複製資料到多個結構中，雖然增加了冗餘和寫入複雜度，但可以避免多次查詢，大幅提高讀取效能。"));

        questions.add(new QuizQuestion(38,
                "設計 Redis Key Schema 時，以下哪個原則最重要？",
                List.of("Key 名稱應具有可讀性且包含業務語義",
                        "Key 名稱越短越好",
                        "Key 名稱應包含隨機字元",
                        "所有 Key 使用同一個前綴"),
                0,
                "好的 Key Schema 應具有可讀性和業務語義，使用冒號分隔的層次結構（如 user:1001:profile），方便維護和除錯。"));

        questions.add(new QuizQuestion(39,
                "在 Redis 中實現次要索引（Secondary Index）的常見方法是？",
                List.of("使用 Set 或 Sorted Set 儲存反向映射",
                        "使用 ALTER TABLE 建立索引",
                        "Redis 不支援次要索引",
                        "使用 KEYS 命令模擬索引"),
                0,
                "可以使用 Set 儲存符合某個條件的 Key 集合（如 status:active -> {user:1, user:2}），或使用 Sorted Set 按分數排序，實現次要索引。"));

        questions.add(new QuizQuestion(40,
                "FT.CREATE 命令中，SCHEMA 定義的作用是？",
                List.of("定義搜尋索引的欄位名稱、類型和索引方式",
                        "建立新的 Redis 資料庫",
                        "定義 ACL 規則",
                        "建立複製拓撲"),
                0,
                "FT.CREATE 的 SCHEMA 部分定義哪些欄位要被索引，以及每個欄位的類型（TEXT、NUMERIC、TAG、GEO、VECTOR 等）和索引方式。"));

        questions.add(new QuizQuestion(41,
                "使用 RedisJSON 儲存文件時，相比 Hash 的主要優勢是？",
                List.of("支援巢狀結構和陣列",
                        "佔用更少記憶體",
                        "讀取速度更快",
                        "不需要序列化"),
                0,
                "RedisJSON 原生支援巢狀的 JSON 物件和陣列，可以直接在伺服器端存取和修改深層巢狀欄位，而 Hash 只支援扁平的欄位-值結構。"));

        questions.add(new QuizQuestion(42,
                "RediSearch 查詢語法中，@category:{electronics} 代表什麼？",
                List.of("在 category TAG 欄位中精確匹配 electronics",
                        "全文搜尋 electronics",
                        "在所有欄位中搜尋 electronics",
                        "刪除 category 欄位"),
                0,
                "@category:{electronics} 是 TAG 欄位的精確匹配語法，只在 category 欄位中匹配值為 electronics 的文件。"));

        questions.add(new QuizQuestion(43,
                "FT.AGGREGATE 與 FT.SEARCH 的主要區別是？",
                List.of("FT.AGGREGATE 支援分組、排序、轉換等聚合操作",
                        "FT.AGGREGATE 更快",
                        "FT.SEARCH 已被棄用",
                        "兩者功能完全相同"),
                0,
                "FT.AGGREGATE 提供了類似 SQL GROUP BY 的聚合管線功能，支援 GROUPBY、REDUCE、SORTBY、APPLY 等操作，而 FT.SEARCH 主要用於搜尋和過濾。"));

        questions.add(new QuizQuestion(44,
                "Redis 向量搜尋（Vector Search）的主要應用場景是？",
                List.of("語義相似度搜尋和推薦系統",
                        "精確字串匹配",
                        "地理位置搜尋",
                        "時間序列分析"),
                0,
                "向量搜尋透過計算向量之間的距離（如餘弦相似度、歐氏距離）找到語義相似的項目，廣泛應用於推薦系統和語義搜尋。"));

        questions.add(new QuizQuestion(45,
                "在 Redis 多模型架構中，同時使用 JSON + Search + TimeSeries 的優勢是？",
                List.of("可以在單一平台上實現文件儲存、全文搜尋和時序分析",
                        "減少記憶體使用",
                        "提高寫入速度",
                        "簡化部署流程"),
                0,
                "Redis 的多模型能力允許在同一平台上結合文件儲存（JSON）、全文搜尋（Search）和時序資料分析（TimeSeries），避免使用多個不同的資料庫。"));

        // =====================================================================
        // Domain 5: Debugging & Troubleshooting (Q46-Q55)
        // =====================================================================

        questions.add(new QuizQuestion(46,
                "SLOWLOG GET 10 命令的作用是？",
                List.of("取得最近 10 筆慢查詢記錄",
                        "取得速度最慢的 10 個 Key",
                        "設定慢查詢閾值為 10 微秒",
                        "取得最近 10 秒的日誌"),
                0,
                "SLOWLOG GET 10 回傳最近 10 筆執行時間超過 slowlog-log-slower-than 閾值的命令記錄，包括執行時間、命令和參數。"));

        questions.add(new QuizQuestion(47,
                "MONITOR 命令的作用和注意事項是？",
                List.of("即時顯示伺服器收到的所有命令，但會顯著影響效能",
                        "監控 Redis 記憶體使用",
                        "監控網路連線狀態",
                        "自動修復效能問題"),
                0,
                "MONITOR 即時輸出 Redis 伺服器收到的所有命令，對除錯非常有用。但它會大幅降低伺服器效能（可達 50%），不應在生產環境長時間使用。"));

        questions.add(new QuizQuestion(48,
                "CLIENT LIST 命令輸出中，idle 欄位代表什麼？",
                List.of("客戶端閒置的秒數", "客戶端的 ID",
                        "客戶端使用的記憶體", "客戶端的連線時間"),
                0,
                "CLIENT LIST 輸出的 idle 欄位表示客戶端自上次發送命令以來的閒置時間（秒數），有助於找出長時間未活動的連線。"));

        questions.add(new QuizQuestion(49,
                "INFO 命令的 keyspace 區段顯示什麼資訊？",
                List.of("每個資料庫中 Key 的數量、有 TTL 的 Key 數量和平均 TTL",
                        "所有 Key 的名稱",
                        "每個 Key 的大小",
                        "Key 的存取頻率"),
                0,
                "INFO keyspace 顯示每個資料庫（db0, db1...）的統計資訊，包括 keys（Key 數量）、expires（有 TTL 的 Key 數量）和 avg_ttl（平均 TTL 毫秒數）。"));

        questions.add(new QuizQuestion(50,
                "MEMORY USAGE key 命令回傳的值包含什麼？",
                List.of("Key 及其值在 Redis 中佔用的總記憶體位元組數",
                        "只有值的大小",
                        "Key 的字串長度",
                        "Key 的過期時間"),
                0,
                "MEMORY USAGE 回傳指定 Key 及其值佔用的總記憶體位元組數，包括 Key 的元資料、指標和值本身的記憶體開銷。"));

        questions.add(new QuizQuestion(51,
                "MEMORY DOCTOR 命令的作用是？",
                List.of("分析 Redis 記憶體使用狀況並提供診斷建議",
                        "釋放未使用的記憶體",
                        "修復記憶體洩漏",
                        "壓縮所有資料"),
                0,
                "MEMORY DOCTOR 會分析 Redis 的記憶體使用模式，並回傳人類可讀的診斷報告和改善建議。"));

        questions.add(new QuizQuestion(52,
                "DEBUG OBJECT key 命令可以查看什麼資訊？",
                List.of("Key 的內部編碼、序列化大小、最近存取時間和引用計數",
                        "Key 的值",
                        "Key 的 ACL 權限",
                        "Key 的複製狀態"),
                0,
                "DEBUG OBJECT 回傳 Key 的內部除錯資訊，包括 encoding（編碼方式）、serializedlength（序列化大小）、lru（LRU 時間）等。"));

        questions.add(new QuizQuestion(53,
                "如何診斷 Redis 的延遲問題？",
                List.of("使用 redis-cli --latency 和 --latency-history 測量延遲",
                        "查看 CPU 使用率",
                        "重啟 Redis 伺服器",
                        "增加記憶體"),
                0,
                "redis-cli --latency 持續測量往返延遲，--latency-history 每 15 秒顯示一次延遲統計。配合 SLOWLOG 和 LATENCY 命令可以全面診斷延遲問題。"));

        questions.add(new QuizQuestion(54,
                "如何偵測 Redis 中的 Big Key？",
                List.of("使用 redis-cli --bigkeys 或 MEMORY USAGE 命令",
                        "使用 KEYS * 列出所有 Key",
                        "查看 Redis 日誌",
                        "使用 CONFIG GET 命令"),
                0,
                "redis-cli --bigkeys 會掃描所有 Key 並報告每種類型中最大的 Key。也可以使用 SCAN 配合 MEMORY USAGE 逐一檢查。"));

        questions.add(new QuizQuestion(55,
                "ACL SETUSER 命令中 ~key:* 的作用是？",
                List.of("允許用戶存取所有以 key: 開頭的 Key",
                        "禁止存取所有 Key",
                        "只允許讀取操作",
                        "設定密碼"),
                0,
                "~key:* 是 Key 模式規則，允許用戶存取所有匹配 key:* 模式的 Key。波浪號（~）表示「允許」該模式的 Key。"));

        // =====================================================================
        // Domain 6: Performance Optimization (Q56-Q70)
        // =====================================================================

        questions.add(new QuizQuestion(56,
                "Redis Pipeline（管線）的主要優勢是？",
                List.of("減少網路往返次數，批次發送多個命令",
                        "保證命令的原子性",
                        "自動重試失敗的命令",
                        "壓縮網路傳輸資料"),
                0,
                "Pipeline 允許客戶端一次發送多個命令而不等待每個命令的回應，大幅減少網路往返（RTT）次數，顯著提升吞吐量。"));

        questions.add(new QuizQuestion(57,
                "MULTI/EXEC 交易的特性是？",
                List.of("所有命令要麼全部執行，要麼全部不執行（原子性）",
                        "支援回滾（Rollback）",
                        "自動加鎖",
                        "可以巢狀使用"),
                0,
                "MULTI/EXEC 保證交易中的命令以原子方式執行（全部執行或全部不執行）。但 Redis 交易不支援回滾，如果某個命令執行失敗，其他命令仍會執行。"));

        questions.add(new QuizQuestion(58,
                "Lua 腳本在 Redis 中執行的特性是？",
                List.of("腳本執行是原子性的，執行期間不會有其他命令插入",
                        "腳本可以被其他命令中斷",
                        "腳本在客戶端執行",
                        "腳本不能存取 Redis 資料"),
                0,
                "Redis Lua 腳本的執行是原子性的，在腳本執行期間，伺服器不會處理任何其他命令。這使得 Lua 腳本適合實現複雜的原子操作。"));

        questions.add(new QuizQuestion(59,
                "連線池（Connection Pool）的主要好處是？",
                List.of("重用已建立的連線，避免頻繁建立和銷毀連線的開銷",
                        "增加安全性",
                        "自動負載平衡",
                        "壓縮傳輸資料"),
                0,
                "連線池預先建立並維護一組 Redis 連線，應用程式可以重用這些連線，避免了每次請求都建立新連線的 TCP 握手和驗證開銷。"));

        questions.add(new QuizQuestion(60,
                "Cache-Aside（旁路快取）模式的流程是？",
                List.of("先查快取，未命中則查資料庫，再將結果寫入快取",
                        "所有寫入先寫快取，再異步寫資料庫",
                        "所有讀取直接從資料庫取，快取自動更新",
                        "快取和資料庫同時寫入"),
                0,
                "Cache-Aside 模式：讀取時先查快取，若未命中則查資料庫並將結果寫入快取。寫入時先更新資料庫，再失效（刪除）快取。"));

        questions.add(new QuizQuestion(61,
                "Read-Through 模式與 Cache-Aside 的區別是？",
                List.of("Read-Through 由快取層自動載入資料，應用程式只與快取互動",
                        "兩者完全相同",
                        "Read-Through 不使用快取",
                        "Cache-Aside 效能更好"),
                0,
                "Read-Through 模式中，快取未命中時由快取層（而非應用程式）自動從資料來源載入資料。應用程式只需要與快取互動，不直接存取資料庫。"));

        questions.add(new QuizQuestion(62,
                "Write-Behind（Write-Back）模式的特點是？",
                List.of("寫入先更新快取，再非同步批次寫入資料庫",
                        "寫入先更新資料庫，再更新快取",
                        "同時寫入快取和資料庫",
                        "只寫入快取，不寫資料庫"),
                0,
                "Write-Behind 模式先將寫入操作更新到快取，然後非同步（延遲或批次）將變更寫入資料庫，可提高寫入效能但有資料遺失風險。"));

        questions.add(new QuizQuestion(63,
                "快取雪崩（Cache Stampede）的解決方案包括？",
                List.of("設定隨機的 TTL 偏移量、使用鎖機制、提前預熱快取",
                        "增加記憶體",
                        "關閉快取",
                        "使用更快的網路"),
                0,
                "快取雪崩的解決方案包括：TTL 加隨機偏移量避免同時過期、使用分散式鎖限制同時重建、提前非同步重建、多層快取等。"));

        questions.add(new QuizQuestion(64,
                "Redis 的 noeviction 淘汰策略在記憶體滿時會怎樣？",
                List.of("拒絕所有寫入命令並回傳 OOM 錯誤",
                        "淘汰最舊的 Key",
                        "自動擴展記憶體",
                        "刪除所有資料"),
                0,
                "noeviction 策略在記憶體達到 maxmemory 限制時，會對所有新的寫入命令回傳 OOM（Out Of Memory）錯誤，但讀取命令仍可正常執行。"));

        questions.add(new QuizQuestion(65,
                "allkeys-lfu 淘汰策略的行為是？",
                List.of("在所有 Key 中淘汰存取頻率最低的 Key",
                        "在所有 Key 中淘汰最近最少使用的 Key",
                        "只淘汰有 TTL 的 Key",
                        "隨機淘汰 Key"),
                0,
                "allkeys-lfu 在所有 Key 中選擇存取頻率（Least Frequently Used）最低的 Key 進行淘汰，適合存取模式不均勻的場景。"));

        questions.add(new QuizQuestion(66,
                "volatile-ttl 淘汰策略的行為是？",
                List.of("在有 TTL 的 Key 中淘汰剩餘生存時間最短的 Key",
                        "在所有 Key 中淘汰 TTL 最長的 Key",
                        "刪除所有有 TTL 的 Key",
                        "只設定 TTL 不淘汰"),
                0,
                "volatile-ttl 在設定了過期時間的 Key 中，優先淘汰剩餘 TTL 最短（最快過期）的 Key。"));

        questions.add(new QuizQuestion(67,
                "maxmemory 配置的作用是？",
                List.of("設定 Redis 可使用的最大記憶體量",
                        "設定每個 Key 的最大大小",
                        "設定連線數上限",
                        "設定持久化檔案大小上限"),
                0,
                "maxmemory 限制 Redis 可使用的最大記憶體量。當達到此限制時，Redis 會根據 maxmemory-policy 設定的淘汰策略處理新的寫入請求。"));

        questions.add(new QuizQuestion(68,
                "lazyfree-lazy-eviction 配置的作用是？",
                List.of("啟用後，記憶體淘汰操作會在背景執行緒中非同步執行",
                        "禁用記憶體淘汰",
                        "延遲所有刪除操作",
                        "使用惰性載入模式"),
                0,
                "lazyfree-lazy-eviction 啟用後，因記憶體淘汰策略需要刪除的 Key 會在背景執行緒中非同步釋放記憶體，避免阻塞主執行緒。"));

        questions.add(new QuizQuestion(69,
                "Client-Side Caching（客戶端快取）的主要機制是？",
                List.of("客戶端在本地快取資料，Redis 透過失效通知告知資料變更",
                        "客戶端自動同步所有 Redis 資料",
                        "在客戶端執行 Redis 命令",
                        "將快取邏輯移到客戶端"),
                0,
                "Client-Side Caching 讓客戶端在本地記憶體中快取資料。Redis 使用追蹤（Tracking）機制，當 Key 被修改時發送失效通知給訂閱的客戶端。"));

        questions.add(new QuizQuestion(70,
                "WAIT numreplicas timeout 命令的作用是？",
                List.of("阻塞直到之前的寫入命令被指定數量的複本確認，或超時",
                        "等待指定時間後執行命令",
                        "等待客戶端連線",
                        "暫停 Redis 伺服器"),
                0,
                "WAIT 命令阻塞當前客戶端，直到之前的所有寫入命令被至少 numreplicas 個複本確認同步，或達到 timeout 毫秒超時。用於提高資料安全性。"));

        // =====================================================================
        // Domain 7: Clustering & High Availability (Q71-Q80)
        // =====================================================================

        questions.add(new QuizQuestion(71,
                "RDB 持久化的主要特點是？",
                List.of("在指定時間間隔建立資料集的完整快照",
                        "記錄每個寫入操作",
                        "即時同步到磁碟",
                        "只儲存 Key 名稱"),
                0,
                "RDB 持久化在設定的時間間隔內對資料集建立時間點快照（Point-in-Time Snapshot），適合備份和災難復原。"));

        questions.add(new QuizQuestion(72,
                "AOF 持久化的 appendfsync always 和 everysec 的區別是？",
                List.of("always 每個寫入命令都同步到磁碟，everysec 每秒同步一次",
                        "兩者完全相同",
                        "always 更快",
                        "everysec 不會遺失資料"),
                0,
                "always 在每個寫入命令後都調用 fsync 同步到磁碟（最安全但最慢）；everysec 每秒調用一次 fsync（預設設定，最多遺失 1 秒資料）。"));

        questions.add(new QuizQuestion(73,
                "Redis 混合持久化（Hybrid Persistence）的工作方式是？",
                List.of("AOF 重寫時先寫入 RDB 格式的資料，再追加重寫期間的 AOF 增量命令",
                        "同時執行 RDB 和 AOF",
                        "交替使用 RDB 和 AOF",
                        "將 RDB 和 AOF 合併為一個檔案"),
                0,
                "混合持久化在 AOF 重寫（BGREWRITEAOF）時，先以 RDB 格式寫入當前資料快照（載入快），再以 AOF 格式追加重寫期間的增量命令。"));

        questions.add(new QuizQuestion(74,
                "Redis Sentinel 的主要功能是？",
                List.of("監控 Redis 主從架構並在主節點故障時自動進行故障轉移",
                        "提供資料分片功能",
                        "壓縮 Redis 資料",
                        "管理 Redis 配置"),
                0,
                "Sentinel 提供高可用性解決方案：監控主從節點健康狀態、在主節點故障時自動選舉新主節點並重新配置從節點（自動故障轉移）。"));

        questions.add(new QuizQuestion(75,
                "Sentinel 判定主節點客觀下線（ODOWN）需要什麼條件？",
                List.of("達到法定人數（quorum）的 Sentinel 節點同意主節點已下線",
                        "一個 Sentinel 判定即可",
                        "所有 Sentinel 同意",
                        "超時 60 秒"),
                0,
                "主觀下線（SDOWN）是單個 Sentinel 判定；客觀下線（ODOWN）需要至少 quorum 數量的 Sentinel 同意主節點已不可達，才會觸發故障轉移。"));

        questions.add(new QuizQuestion(76,
                "Redis Cluster 使用多少個 Hash Slot 進行資料分片？",
                List.of("1024", "4096", "16384", "65536"),
                2,
                "Redis Cluster 使用 16384 個 Hash Slot（0-16383）進行資料分片。每個 Key 透過 CRC16(key) mod 16384 映射到對應的 Slot。"));

        questions.add(new QuizQuestion(77,
                "Redis Cluster 中收到 MOVED 重定向的含義是？",
                List.of("請求的 Key 所屬的 Slot 已永久移動到另一個節點",
                        "Key 已被刪除",
                        "Slot 正在遷移中",
                        "需要重新驗證"),
                0,
                "MOVED 重定向表示請求的 Key 所屬的 Hash Slot 已永久分配到另一個節點。客戶端應更新本地的 Slot-Node 映射表並重新發送請求。"));

        questions.add(new QuizQuestion(78,
                "Redis Cluster 中 ASK 重定向與 MOVED 的區別是？",
                List.of("ASK 表示 Slot 正在遷移中，只需臨時重定向一次",
                        "兩者完全相同",
                        "ASK 表示永久遷移",
                        "ASK 用於讀取，MOVED 用於寫入"),
                0,
                "ASK 表示 Slot 正在從一個節點遷移到另一個節點的過程中，客戶端只需要將當前這一次請求重定向到目標節點，不應更新本地映射。MOVED 是永久重定向。"));

        questions.add(new QuizQuestion(79,
                "Redis Cluster 進行 Resharding（重新分片）時，資料如何遷移？",
                List.of("使用 MIGRATE 命令將 Slot 中的 Key 從來源節點逐個遷移到目標節點",
                        "一次性複製所有資料",
                        "重啟叢集",
                        "自動完成，不需要遷移"),
                0,
                "Resharding 將 Hash Slot 從一個節點重新分配到另一個節點。過程中使用 CLUSTER SETSLOT、MIGRATE 等命令逐步遷移 Slot 中的 Key。"));

        questions.add(new QuizQuestion(80,
                "Redis Cluster 中防止腦裂（Split-Brain）的機制是？",
                List.of("少數派分區中的主節點停止接受寫入（cluster-node-timeout + 複本投票機制）",
                        "自動合併衝突資料",
                        "使用版本向量",
                        "Redis 不處理腦裂問題"),
                0,
                "Redis Cluster 透過要求大多數主節點可達才能進行故障轉移，以及 cluster-node-timeout 機制讓少數派分區中的主節點停止接受寫入，來防止腦裂問題。"));

        return new Quiz("Redis 認證模擬考試", "module-14", questions, 0.8);
    }

    @Test
    @DisplayName("allCorrectAnswers_ScorePerfect — 全部答對應獲得滿分")
    void allCorrectAnswers_ScorePerfect() {
        Quiz quiz = createCertificationExam();
        Map<Integer, Integer> allCorrect = new HashMap<>();
        for (QuizQuestion q : quiz.questions()) {
            allCorrect.put(q.number(), q.correctOptionIndex());
        }
        QuizResult result = QuizRunner.run(quiz, allCorrect);
        assertThat(result.passed()).isTrue();
        assertThat(result.totalQuestions()).isEqualTo(80);
        assertThat(result.score()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("noAnswers_ScoreZero — 全部未答應得零分且不及格")
    void noAnswers_ScoreZero() {
        Quiz quiz = createCertificationExam();
        QuizResult result = QuizRunner.run(quiz, Map.of());
        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isZero();
    }

    @Test
    @DisplayName("examHas80Questions — 考試應有 80 題")
    void examHas80Questions() {
        Quiz quiz = createCertificationExam();
        assertThat(quiz.questions()).hasSize(80);
    }

    @Test
    @DisplayName("passingRequires80Percent — 及格門檻為 80%")
    void passingRequires80Percent() {
        Quiz quiz = createCertificationExam();
        assertThat(quiz.passingRate()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("exactly80PercentPasses — 剛好 80% 正確率應及格")
    void exactly80PercentPasses() {
        Quiz quiz = createCertificationExam();
        Map<Integer, Integer> answers = new HashMap<>();
        // Answer first 64 correctly (64/80 = 0.8)
        for (int i = 1; i <= 64; i++) {
            answers.put(i, quiz.questions().get(i - 1).correctOptionIndex());
        }
        // Answer remaining 16 incorrectly
        for (int i = 65; i <= 80; i++) {
            int correct = quiz.questions().get(i - 1).correctOptionIndex();
            answers.put(i, (correct + 1) % 4);
        }
        QuizResult result = QuizRunner.run(quiz, answers);
        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(64);
        assertThat(result.score()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("below80PercentFails — 低於 80% 正確率應不及格")
    void below80PercentFails() {
        Quiz quiz = createCertificationExam();
        Map<Integer, Integer> answers = new HashMap<>();
        // Answer first 63 correctly (63/80 = 0.7875 < 0.8)
        for (int i = 1; i <= 63; i++) {
            answers.put(i, quiz.questions().get(i - 1).correctOptionIndex());
        }
        // Answer remaining 17 incorrectly
        for (int i = 64; i <= 80; i++) {
            int correct = quiz.questions().get(i - 1).correctOptionIndex();
            answers.put(i, (correct + 1) % 4);
        }
        QuizResult result = QuizRunner.run(quiz, answers);
        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(63);
    }

    @Test
    @DisplayName("allQuestionNumbersUnique — 所有題號應唯一")
    void allQuestionNumbersUnique() {
        Quiz quiz = createCertificationExam();
        List<Integer> numbers = quiz.questions().stream()
                .map(QuizQuestion::number)
                .toList();
        assertThat(numbers).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("allQuestionsHaveFourOptions — 每題應有四個選項")
    void allQuestionsHaveFourOptions() {
        Quiz quiz = createCertificationExam();
        for (QuizQuestion q : quiz.questions()) {
            assertThat(q.options())
                    .as("Question %d should have 4 options", q.number())
                    .hasSize(4);
        }
    }

    @Test
    @DisplayName("allCorrectOptionIndicesValid — 所有正確答案索引應在有效範圍內")
    void allCorrectOptionIndicesValid() {
        Quiz quiz = createCertificationExam();
        for (QuizQuestion q : quiz.questions()) {
            assertThat(q.correctOptionIndex())
                    .as("Question %d correct index should be 0-3", q.number())
                    .isBetween(0, 3);
        }
    }
}
