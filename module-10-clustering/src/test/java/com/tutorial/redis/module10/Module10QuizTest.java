package com.tutorial.redis.module10;

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

/**
 * 測試 Module 10 的隨堂測驗功能。
 * 驗證 Redis Cluster 相關知識點：Hash Slot、CRC16、Hash Tag、MOVED/ASK 重定向、Gossip 協議等。
 * 本測試類別不屬於特定架構層，而是驗證整體模組的學習成果評量機制。
 */
@DisplayName("Module 10 Quiz — Redis Cluster 與水平擴展")
class Module10QuizTest {

    private Quiz quiz;

    @BeforeEach
    void setUp() {
        quiz = new Quiz(
                "Module 10 — Redis Cluster 與水平擴展",
                "module-10",
                List.of(
                        new QuizQuestion(1,
                                "Redis Cluster 共有多少個 Hash Slot?",
                                List.of("16384", "65536", "1024", "4096"),
                                0,
                                "Redis Cluster 使用 16384 個 Hash Slot 來分配 Key，每個 Master 節點負責一部分 Slot。"),
                        new QuizQuestion(2,
                                "Redis Cluster 使用什麼演算法計算 Key 的 Hash Slot?",
                                List.of("CRC16", "MD5", "SHA-256", "Murmur3"),
                                0,
                                "Redis Cluster 使用 CRC16 演算法計算 Key 的雜湊值，再對 16384 取模得到 Slot 編號。"),
                        new QuizQuestion(3,
                                "Hash Tag 的語法是?",
                                List.of("{tag}:key — 用大括號包住的部分決定 Slot", "tag#key", "tag:key", "[tag]:key"),
                                0,
                                "Hash Tag 使用大括號 {} 語法，只有大括號內的部分會用於 CRC16 計算，確保相關 Key 落在同一 Slot。"),
                        new QuizQuestion(4,
                                "Redis Cluster 中 -MOVED 錯誤代表?",
                                List.of("Key 所在的 Slot 已永久轉移到另一個節點", "節點暫時不可用", "Key 不存在", "叢集正在重啟"),
                                0,
                                "-MOVED 錯誤表示該 Slot 已永久遷移到另一個節點，客戶端應更新 Slot 映射表並重新發送請求。"),
                        new QuizQuestion(5,
                                "為什麼多 Key 操作在 Cluster 中有限制?",
                                List.of("多個 Key 可能分布在不同節點，無法原子操作", "效能考量", "安全限制", "記憶體限制"),
                                0,
                                "Redis Cluster 中多 Key 操作要求所有 Key 在同一 Slot，因為不同 Slot 可能分布在不同節點上，無法保證原子性。"),
                        new QuizQuestion(6,
                                "Hash Tag 的用途是?",
                                List.of("確保相關的 Key 分配到相同的 Slot", "加速 Hash 計算", "加密 Key", "壓縮 Key"),
                                0,
                                "Hash Tag 讓多個相關的 Key 使用相同的雜湊輸入，確保它們被分配到同一個 Slot，從而支援多 Key 操作。"),
                        new QuizQuestion(7,
                                "Redis Cluster 最少需要幾個 Master 節點?",
                                List.of("3", "1", "2", "6"),
                                0,
                                "Redis Cluster 至少需要 3 個 Master 節點，以支援多數決投票機制進行故障偵測和自動故障轉移。"),
                        new QuizQuestion(8,
                                "-ASK 重定向與 -MOVED 的差別是?",
                                List.of("-ASK 是暫時性的 (Slot 遷移中)，-MOVED 是永久性的", "兩者相同", "-ASK 更嚴重", "-MOVED 是暫時性的"),
                                0,
                                "-ASK 表示 Slot 正在遷移中，客戶端應先發送 ASKING 命令再重試；-MOVED 表示 Slot 已永久遷移，客戶端應更新映射表。"),
                        new QuizQuestion(9,
                                "Gossip 協議在 Redis Cluster 中的作用是?",
                                List.of("節點間交換狀態資訊", "客戶端負載均衡", "資料加密", "日誌同步"),
                                0,
                                "Redis Cluster 使用 Gossip 協議讓節點間定期交換狀態資訊，包括節點存活狀態、Slot 分配等，實現去中心化的叢集管理。"),
                        new QuizQuestion(10,
                                "cluster-node-timeout 配置的作用是?",
                                List.of("節點被判定為失效的超時時間", "客戶端連線超時", "命令執行超時", "Slot 遷移超時"),
                                0,
                                "cluster-node-timeout 設定節點無回應多久後被標記為 PFAIL/FAIL，預設 15000 毫秒，影響故障偵測速度。")
                )
        );
    }

    // 驗證所有 10 題全部答對時，測驗結果為通過且得分為滿分 (1.0)
    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        // Arrange — all correct answers at index 0
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // 16384
        answers.put(2, 0);   // CRC16
        answers.put(3, 0);   // {tag}:key — 用大括號包住的部分決定 Slot
        answers.put(4, 0);   // Key 所在的 Slot 已永久轉移到另一個節點
        answers.put(5, 0);   // 多個 Key 可能分布在不同節點，無法原子操作
        answers.put(6, 0);   // 確保相關的 Key 分配到相同的 Slot
        answers.put(7, 0);   // 3
        answers.put(8, 0);   // -ASK 是暫時性的 (Slot 遷移中)，-MOVED 是永久性的
        answers.put(9, 0);   // 節點間交換狀態資訊
        answers.put(10, 0);  // 節點被判定為失效的超時時間

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(10);
    }

    // 驗證答對 8 題 (80%) 時，測驗結果仍為通過（及格門檻為 80%）
    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 8/10 正確")
    void quiz_PassesAt80Percent() {
        // Arrange — 8 correct + 2 wrong
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct — 16384
        answers.put(2, 0);   // correct — CRC16
        answers.put(3, 0);   // correct — {tag}:key
        answers.put(4, 0);   // correct — Key 所在的 Slot 已永久轉移到另一個節點
        answers.put(5, 0);   // correct — 多個 Key 可能分布在不同節點，無法原子操作
        answers.put(6, 0);   // correct — 確保相關的 Key 分配到相同的 Slot
        answers.put(7, 0);   // correct — 3
        answers.put(8, 0);   // correct — -ASK 是暫時性的 (Slot 遷移中)，-MOVED 是永久性的
        answers.put(9, 1);   // wrong — chose "客戶端負載均衡" instead of "節點間交換狀態資訊"
        answers.put(10, 1);  // wrong — chose "客戶端連線超時" instead of "節點被判定為失效的超時時間"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(8);
        assertThat(result.score()).isEqualTo(8.0 / 10.0);
    }

    // 驗證答對 7 題 (70%) 時，測驗結果為不通過（低於 80% 及格門檻）
    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 7/10 正確")
    void quiz_FailsBelow80Percent() {
        // Arrange — 7 correct + 3 wrong
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct — 16384
        answers.put(2, 0);   // correct — CRC16
        answers.put(3, 0);   // correct — {tag}:key
        answers.put(4, 0);   // correct — Key 所在的 Slot 已永久轉移到另一個節點
        answers.put(5, 0);   // correct — 多個 Key 可能分布在不同節點，無法原子操作
        answers.put(6, 0);   // correct — 確保相關的 Key 分配到相同的 Slot
        answers.put(7, 0);   // correct — 3
        answers.put(8, 1);   // wrong — chose "兩者相同" instead of "-ASK 是暫時性的"
        answers.put(9, 1);   // wrong — chose "客戶端負載均衡" instead of "節點間交換狀態資訊"
        answers.put(10, 1);  // wrong — chose "客戶端連線超時" instead of "節點被判定為失效的超時時間"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(7);
        assertThat(result.score()).isEqualTo(7.0 / 10.0);
    }
}
