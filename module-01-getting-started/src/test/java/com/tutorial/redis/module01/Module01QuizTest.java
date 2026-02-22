package com.tutorial.redis.module01;

import com.tutorial.redis.common.quiz.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 01 隨堂測驗測試
 * 驗證學員對 Redis 入門知識的掌握程度，包含 port、資料存儲方式、
 * 單執行緒架構、Lettuce/Jedis 差異、Key 命名慣例與 TTL 等核心觀念。
 * 層級：模組層級測驗（跨層級概念驗證）
 */
@DisplayName("Module 01 Quiz — Redis 入門與連線管理")
class Module01QuizTest {

    private static final Quiz QUIZ = new Quiz(
            "Redis 入門與連線管理",
            "module-01",
            List.of(
                    new QuizQuestion(1,
                            "Redis 預設監聽的 port 是？",
                            List.of("3306", "5432", "6379", "27017"),
                            2,
                            "Redis 預設使用 port 6379。"),
                    new QuizQuestion(2,
                            "Redis 的主要資料存儲方式是？",
                            List.of("磁碟優先", "記憶體優先", "網路優先", "混合優先"),
                            1,
                            "Redis 是 in-memory 資料存儲，資料主要存放在記憶體中。"),
                    new QuizQuestion(3,
                            "Redis 使用什麼架構處理命令？",
                            List.of("Multi-threaded", "Single-threaded + I/O Multiplexing", "Actor Model", "Fork-Join"),
                            1,
                            "Redis 使用單執行緒處理命令，搭配 I/O Multiplexing (epoll/kqueue) 處理網路連線。"),
                    new QuizQuestion(4,
                            "Lettuce 與 Jedis 的主要差異是？",
                            List.of("Lettuce 是同步的", "Lettuce 是 blocking I/O", "Lettuce 是非阻塞且執行緒安全的", "Jedis 比 Lettuce 快"),
                            2,
                            "Lettuce 基於 Netty，支援非阻塞 I/O 且是執行緒安全的，可共享單一連線。"),
                    new QuizQuestion(5,
                            "在生產環境中，應避免使用哪個命令來列出所有 key？",
                            List.of("SCAN", "KEYS *", "EXISTS", "TYPE"),
                            1,
                            "KEYS * 會阻塞 Redis 直到遍歷完所有 key，應使用 SCAN 漸進式遍歷替代。"),
                    new QuizQuestion(6,
                            "Redis Key 命名慣例中，建議使用什麼符號作為分隔？",
                            List.of("底線 _", "冒號 :", "斜線 /", "點號 ."),
                            1,
                            "Redis 社群標準使用冒號 (:) 作為 key 的層級分隔符號。"),
                    new QuizQuestion(7,
                            "StringRedisTemplate 與 RedisTemplate<String, Object> 的主要差異是？",
                            List.of("StringRedisTemplate 只能存 String 值", "RedisTemplate 不支援 Hash", "StringRedisTemplate 較慢", "兩者完全相同"),
                            0,
                            "StringRedisTemplate 的 key 和 value 都使用 StringRedisSerializer，只處理 String 類型。"),
                    new QuizQuestion(8,
                            "SET 命令的時間複雜度是？",
                            List.of("O(N)", "O(log N)", "O(1)", "O(N log N)"),
                            2,
                            "SET 命令的時間複雜度為 O(1)，是常數時間操作。"),
                    new QuizQuestion(9,
                            "TTL 命令回傳 -2 代表什麼？",
                            List.of("Key 永不過期", "Key 已過期或不存在", "Key 剩餘 2 秒", "發生錯誤"),
                            1,
                            "TTL 回傳 -2 表示 key 不存在（已過期或從未設定）；回傳 -1 表示 key 存在但未設定過期。"),
                    new QuizQuestion(10,
                            "Redis 連線池中，maxTotal 參數代表？",
                            List.of("最大閒置連線數", "最小閒置連線數", "最大連線總數", "最大等待時間"),
                            2,
                            "maxTotal 控制連線池中允許的最大連線總數，包含活躍與閒置連線。")
            )
    );

    // 驗證全部答對時測驗通過且分數為滿分
    @Test
    @DisplayName("Quiz 通過率需達 80%（滿分驗證）")
    void quiz_WhenAllCorrect_Passes() {
        // All correct answers
        Map<Integer, Integer> answers = Map.of(
                1, 2, 2, 1, 3, 1, 4, 2, 5, 1,
                6, 1, 7, 0, 8, 2, 9, 1, 10, 2
        );

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(10);
    }

    // 驗證答對 80% 時仍可通過測驗門檻
    @Test
    @DisplayName("Quiz 80% 正確仍通過")
    void quiz_When80Percent_StillPasses() {
        // 8 correct, 2 wrong
        Map<Integer, Integer> answers = Map.of(
                1, 2, 2, 1, 3, 1, 4, 2, 5, 1,
                6, 1, 7, 0, 8, 2, 9, 0, 10, 0
        );

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(8);
    }

    // 驗證答對率低於 80% 時測驗不通過
    @Test
    @DisplayName("Quiz 低於 80% 不通過")
    void quiz_WhenBelow80Percent_Fails() {
        // Only 7 correct
        Map<Integer, Integer> answers = Map.of(
                1, 2, 2, 1, 3, 1, 4, 2, 5, 1,
                6, 1, 7, 0, 8, 0, 9, 0, 10, 0
        );

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(7);
    }
}
