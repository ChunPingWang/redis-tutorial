package com.tutorial.redis.module13;

import com.tutorial.redis.common.quiz.Quiz;
import com.tutorial.redis.common.quiz.QuizQuestion;
import com.tutorial.redis.common.quiz.QuizResult;
import com.tutorial.redis.common.quiz.QuizRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Module 13 Quiz — 安全、監控與生產最佳實踐")
class Module13QuizTest {

    private Quiz quiz;

    @BeforeEach
    void setUp() {
        quiz = new Quiz(
                "Module 13 — 安全、監控與生產最佳實踐",
                "module-13",
                List.of(
                        new QuizQuestion(1,
                                "Redis ACL SETUSER 命令的作用是?",
                                List.of("建立或修改用戶的存取權限", "設定密碼", "建立資料庫", "設定超時"),
                                0,
                                "ACL SETUSER 用於建立新的 ACL 用戶或修改現有用戶的權限，包括命令存取、Key 模式和密碼設定。"),
                        new QuizQuestion(2,
                                "noeviction 淘汰策略在記憶體滿時會?",
                                List.of("拒絕新的寫入操作", "淘汰最久未使用的 Key", "淘汰最少使用的 Key", "隨機淘汰"),
                                0,
                                "noeviction 策略在記憶體達到 maxmemory 限制時，會直接拒絕所有新的寫入操作，回傳 OOM 錯誤。"),
                        new QuizQuestion(3,
                                "allkeys-lru 和 volatile-lru 的差別是?",
                                List.of("allkeys 淘汰所有 Key，volatile 只淘汰有 TTL 的 Key", "效能不同", "兩者相同", "allkeys 更安全"),
                                0,
                                "allkeys-lru 在所有 Key 中選擇最近最少使用的進行淘汰；volatile-lru 只在設定了 TTL 的 Key 中選擇。"),
                        new QuizQuestion(4,
                                "SLOWLOG GET 命令的作用是?",
                                List.of("取得慢查詢日誌", "取得錯誤日誌", "取得存取日誌", "取得複製日誌"),
                                0,
                                "SLOWLOG GET 用於取得 Redis 慢查詢日誌，記錄執行時間超過 slowlog-log-slower-than 設定值的命令。"),
                        new QuizQuestion(5,
                                "Redis INFO memory 中 used_memory 代表?",
                                List.of("Redis 已使用的記憶體量", "系統總記憶體", "可用記憶體", "記憶體上限"),
                                0,
                                "used_memory 表示 Redis 分配器（如 jemalloc）目前已分配的記憶體總量，以位元組為單位。"),
                        new QuizQuestion(6,
                                "Cache Hit Rate 的計算公式是?",
                                List.of("hits / (hits + misses)", "hits / total_commands", "hits / connected_clients", "hits - misses"),
                                0,
                                "快取命中率 = keyspace_hits / (keyspace_hits + keyspace_misses)，反映 Redis 作為快取的效率。"),
                        new QuizQuestion(7,
                                "protected-mode 配置的作用是?",
                                List.of("限制只接受本地連線（無密碼時）", "加密所有連線", "阻擋所有連線", "啟用 TLS"),
                                0,
                                "protected-mode 啟用時，若沒有設定密碼且沒有綁定特定 IP，Redis 只接受來自 127.0.0.1 的連線。"),
                        new QuizQuestion(8,
                                "rename-command FLUSHALL \"\" 的作用是?",
                                List.of("停用 FLUSHALL 命令", "重新命名 FLUSHALL", "執行 FLUSHALL", "備份後執行 FLUSHALL"),
                                0,
                                "將 FLUSHALL 命令重新命名為空字串等同於停用該命令，是一種安全防護措施。"),
                        new QuizQuestion(9,
                                "MEMORY USAGE <key> 命令的作用是?",
                                List.of("查詢指定 Key 佔用的記憶體位元組", "刪除 Key 的記憶體", "限制 Key 的記憶體", "壓縮 Key 的記憶體"),
                                0,
                                "MEMORY USAGE 回傳指定 Key 及其值在 Redis 中佔用的記憶體位元組數，有助於找出 Big Key。"),
                        new QuizQuestion(10,
                                "CLIENT LIST 命令顯示的資訊包括?",
                                List.of("所有客戶端連線的詳細資訊", "Redis 配置", "Key 列表", "記憶體統計"),
                                0,
                                "CLIENT LIST 回傳所有已連線客戶端的詳細資訊，包括 IP、port、idle 時間、命令等。"),
                        new QuizQuestion(11,
                                "LFU (Least Frequently Used) 和 LRU (Least Recently Used) 的差別是?",
                                List.of("LFU 淘汰使用次數最少的，LRU 淘汰最久未使用的", "兩者相同", "LFU 更快", "LRU 使用更少記憶體"),
                                0,
                                "LFU 根據存取頻率淘汰使用次數最少的 Key；LRU 根據最後存取時間淘汰最久未被存取的 Key。"),
                        new QuizQuestion(12,
                                "TLS 在 Redis 中的配置需要哪些檔案?",
                                List.of("tls-cert-file、tls-key-file、tls-ca-cert-file", "只需要密碼", "只需要 cert-file", "不需要任何檔案"),
                                0,
                                "Redis TLS 配置需要伺服器憑證 (tls-cert-file)、私鑰 (tls-key-file) 和 CA 憑證 (tls-ca-cert-file)。"),
                        new QuizQuestion(13,
                                "Big Key 偵測最常用的方法是?",
                                List.of("redis-cli --bigkeys", "redis-cli --scan", "INFO keyspace", "KEYS *"),
                                0,
                                "redis-cli --bigkeys 會掃描所有 Key 並報告每種資料類型中最大的 Key，是偵測 Big Key 的常用方法。"),
                        new QuizQuestion(14,
                                "instantaneous_ops_per_sec 指標代表?",
                                List.of("Redis 目前每秒處理的命令數", "Redis 啟動以來的總命令數", "Redis 最大命令處理能力", "客戶端每秒發送的命令數"),
                                0,
                                "instantaneous_ops_per_sec 是 Redis 即時統計的每秒命令處理數，反映當前的負載水平。"),
                        new QuizQuestion(15,
                                "生產環境中 Redis 不應該暴露在公網的原因是?",
                                List.of("Redis 設計為內網使用，公網暴露會有安全風險", "效能會降低", "不支援公網", "授權限制"),
                                0,
                                "Redis 設計為在受信任的內網環境中使用，公網暴露會面臨未授權存取、資料外洩等嚴重安全風險。")
                )
        );
    }

    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        // Arrange — all correct answers at index 0
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // 建立或修改用戶的存取權限
        answers.put(2, 0);   // 拒絕新的寫入操作
        answers.put(3, 0);   // allkeys 淘汰所有 Key，volatile 只淘汰有 TTL 的 Key
        answers.put(4, 0);   // 取得慢查詢日誌
        answers.put(5, 0);   // Redis 已使用的記憶體量
        answers.put(6, 0);   // hits / (hits + misses)
        answers.put(7, 0);   // 限制只接受本地連線（無密碼時）
        answers.put(8, 0);   // 停用 FLUSHALL 命令
        answers.put(9, 0);   // 查詢指定 Key 佔用的記憶體位元組
        answers.put(10, 0);  // 所有客戶端連線的詳細資訊
        answers.put(11, 0);  // LFU 淘汰使用次數最少的，LRU 淘汰最久未使用的
        answers.put(12, 0);  // tls-cert-file、tls-key-file、tls-ca-cert-file
        answers.put(13, 0);  // redis-cli --bigkeys
        answers.put(14, 0);  // Redis 目前每秒處理的命令數
        answers.put(15, 0);  // Redis 設計為內網使用，公網暴露會有安全風險

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(15);
    }

    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 12/15 正確")
    void quiz_PassesAt80Percent() {
        // Arrange — 12 correct + 3 wrong
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct — 建立或修改用戶的存取權限
        answers.put(2, 0);   // correct — 拒絕新的寫入操作
        answers.put(3, 0);   // correct — allkeys 淘汰所有 Key，volatile 只淘汰有 TTL 的 Key
        answers.put(4, 0);   // correct — 取得慢查詢日誌
        answers.put(5, 0);   // correct — Redis 已使用的記憶體量
        answers.put(6, 0);   // correct — hits / (hits + misses)
        answers.put(7, 0);   // correct — 限制只接受本地連線（無密碼時）
        answers.put(8, 0);   // correct — 停用 FLUSHALL 命令
        answers.put(9, 0);   // correct — 查詢指定 Key 佔用的記憶體位元組
        answers.put(10, 0);  // correct — 所有客戶端連線的詳細資訊
        answers.put(11, 0);  // correct — LFU 淘汰使用次數最少的，LRU 淘汰最久未使用的
        answers.put(12, 0);  // correct — tls-cert-file、tls-key-file、tls-ca-cert-file
        answers.put(13, 1);  // wrong — chose "redis-cli --scan" instead of "redis-cli --bigkeys"
        answers.put(14, 1);  // wrong — chose "Redis 啟動以來的總命令數" instead of "Redis 目前每秒處理的命令數"
        answers.put(15, 1);  // wrong — chose "效能會降低" instead of "Redis 設計為內網使用"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert — 12/15 = 0.8 which equals the passing threshold
        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(12);
        assertThat(result.score()).isCloseTo(12.0 / 15.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 11/15 正確")
    void quiz_FailsBelow80Percent() {
        // Arrange — 11 correct + 4 wrong (11/15 = 0.733... < 0.8)
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct — 建立或修改用戶的存取權限
        answers.put(2, 0);   // correct — 拒絕新的寫入操作
        answers.put(3, 0);   // correct — allkeys 淘汰所有 Key，volatile 只淘汰有 TTL 的 Key
        answers.put(4, 0);   // correct — 取得慢查詢日誌
        answers.put(5, 0);   // correct — Redis 已使用的記憶體量
        answers.put(6, 0);   // correct — hits / (hits + misses)
        answers.put(7, 0);   // correct — 限制只接受本地連線（無密碼時）
        answers.put(8, 0);   // correct — 停用 FLUSHALL 命令
        answers.put(9, 0);   // correct — 查詢指定 Key 佔用的記憶體位元組
        answers.put(10, 0);  // correct — 所有客戶端連線的詳細資訊
        answers.put(11, 0);  // correct — LFU 淘汰使用次數最少的，LRU 淘汰最久未使用的
        answers.put(12, 1);  // wrong — chose "只需要密碼" instead of "tls-cert-file..."
        answers.put(13, 1);  // wrong — chose "redis-cli --scan" instead of "redis-cli --bigkeys"
        answers.put(14, 1);  // wrong — chose "Redis 啟動以來的總命令數" instead of "Redis 目前每秒處理的命令數"
        answers.put(15, 1);  // wrong — chose "效能會降低" instead of "Redis 設計為內網使用"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert — 11/15 = 0.733... which is below 0.8
        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(11);
        assertThat(result.score()).isEqualTo(11.0 / 15.0);
    }
}
