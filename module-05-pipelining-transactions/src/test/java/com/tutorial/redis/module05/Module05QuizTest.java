package com.tutorial.redis.module05;

import com.tutorial.redis.common.quiz.Quiz;
import com.tutorial.redis.common.quiz.QuizQuestion;
import com.tutorial.redis.common.quiz.QuizResult;
import com.tutorial.redis.common.quiz.QuizRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Module 05 Quiz — Pipeline 與交易")
class Module05QuizTest {

    private static final Quiz QUIZ = new Quiz(
            "Pipeline 與交易",
            "module-05",
            List.of(
                    new QuizQuestion(1,
                            "Pipeline 主要減少什麼？",
                            List.of(
                                    "Redis 記憶體使用量",
                                    "網路 RTT 往返次數",
                                    "CPU 運算時間",
                                    "磁碟 I/O 次數"
                            ),
                            1,
                            "Pipeline 將多個命令批次傳送到 Redis，減少網路 RTT（Round-Trip Time）往返次數，而非減少記憶體或 CPU 使用。"),
                    new QuizQuestion(2,
                            "MULTI/EXEC 內的命令何時執行？",
                            List.of(
                                    "MULTI 時立即執行",
                                    "每個命令逐一執行",
                                    "EXEC 時一次性執行",
                                    "DISCARD 時執行"
                            ),
                            2,
                            "MULTI 開始事務後命令會排入佇列，直到 EXEC 時才一次性原子執行所有排隊的命令。"),
                    new QuizQuestion(3,
                            "WATCH 的用途是？",
                            List.of(
                                    "監控 Redis 伺服器效能",
                                    "樂觀鎖，監視 key 變更則 EXEC 失敗",
                                    "悲觀鎖，阻塞其他客戶端",
                                    "監聽 Pub/Sub 頻道"
                            ),
                            1,
                            "WATCH 實現樂觀鎖機制：監視指定 key，若在 EXEC 之前被其他客戶端修改，則事務自動失敗回傳 null。"),
                    new QuizQuestion(4,
                            "Lua Script 在 Redis 中的執行特性？",
                            List.of(
                                    "多執行緒並行執行",
                                    "原子性，阻塞其他命令直到完成",
                                    "非同步背景執行",
                                    "可被其他命令中斷"
                            ),
                            1,
                            "Redis 以單執行緒方式執行 Lua 腳本，具有原子性，執行期間會阻塞其他命令直到腳本完成。"),
                    new QuizQuestion(5,
                            "Pipeline vs MULTI/EXEC 差異？",
                            List.of(
                                    "Pipeline 有原子性，MULTI/EXEC 無原子性",
                                    "兩者完全相同",
                                    "Pipeline 無原子性，MULTI/EXEC 有弱原子性",
                                    "Pipeline 支援回滾，MULTI/EXEC 不支援"
                            ),
                            2,
                            "Pipeline 只是批次傳送命令以減少 RTT，不保證原子性；MULTI/EXEC 保證命令一次性執行的弱原子性（無回滾）。"),
                    new QuizQuestion(6,
                            "WATCH + MULTI/EXEC 失敗時 EXEC 回傳什麼？",
                            List.of(
                                    "空陣列 []",
                                    "錯誤訊息",
                                    "null",
                                    "false"
                            ),
                            2,
                            "當 WATCH 監視的 key 在 EXEC 前被修改，EXEC 回傳 null（在 Jedis/Lettuce 中為 null），表示事務被中止。"),
                    new QuizQuestion(7,
                            "Lua 腳本防超賣的原理？",
                            List.of(
                                    "使用分散式鎖",
                                    "使用悲觀鎖定",
                                    "原子性地檢查庫存 + 扣減",
                                    "使用資料庫交易"
                            ),
                            2,
                            "Lua 腳本在 Redis 中原子性執行，將「檢查庫存是否足夠」與「扣減庫存」合併為不可分割的操作，杜絕並發超賣。"),
                    new QuizQuestion(8,
                            "Pipeline 適合什麼場景？",
                            List.of(
                                    "需要嚴格原子性的操作",
                                    "單一命令的即時查詢",
                                    "大量獨立讀寫操作的批次處理",
                                    "需要 key 監視的併發控制"
                            ),
                            2,
                            "Pipeline 最適合大量不需要互相依賴的獨立讀寫操作，透過批次傳送減少網路往返次數來提升吞吐量。"),
                    new QuizQuestion(9,
                            "EVALSHA 相比 EVAL 的優勢？",
                            List.of(
                                    "執行速度更快",
                                    "支援更多 Lua 語法",
                                    "避免重複傳送 Lua 腳本內容，使用 SHA1 hash",
                                    "支援多執行緒執行"
                            ),
                            2,
                            "EVALSHA 使用腳本的 SHA1 hash 呼叫已快取的 Lua 腳本，避免每次都傳送完整腳本內容，節省網路頻寬。"),
                    new QuizQuestion(10,
                            "Redis Transaction 不支援什麼？",
                            List.of(
                                    "多命令排隊",
                                    "樂觀鎖 WATCH",
                                    "回滾 Rollback",
                                    "原子性執行"
                            ),
                            2,
                            "Redis Transaction (MULTI/EXEC) 不支援回滾。即使事務中某個命令執行失敗，其他命令仍會繼續執行，不會自動回滾。")
            )
    );

    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 1);   // 網路 RTT 往返次數
        answers.put(2, 2);   // EXEC 時一次性執行
        answers.put(3, 1);   // 樂觀鎖，監視 key 變更則 EXEC 失敗
        answers.put(4, 1);   // 原子性，阻塞其他命令直到完成
        answers.put(5, 2);   // Pipeline 無原子性，MULTI/EXEC 有弱原子性
        answers.put(6, 2);   // null
        answers.put(7, 2);   // 原子性地檢查庫存 + 扣減
        answers.put(8, 2);   // 大量獨立讀寫操作的批次處理
        answers.put(9, 2);   // 避免重複傳送 Lua 腳本內容，使用 SHA1 hash
        answers.put(10, 2);  // 回滾 Rollback

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(10);
    }

    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 8/10 正確")
    void quiz_PassesAt80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 1);   // correct
        answers.put(2, 2);   // correct
        answers.put(3, 1);   // correct
        answers.put(4, 1);   // correct
        answers.put(5, 2);   // correct
        answers.put(6, 2);   // correct
        answers.put(7, 2);   // correct
        answers.put(8, 2);   // correct
        answers.put(9, 0);   // wrong
        answers.put(10, 0);  // wrong

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(8);
        assertThat(result.score()).isEqualTo(8.0 / 10.0);
    }

    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 7/10 正確")
    void quiz_FailsBelow80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 1);   // correct
        answers.put(2, 2);   // correct
        answers.put(3, 1);   // correct
        answers.put(4, 1);   // correct
        answers.put(5, 2);   // correct
        answers.put(6, 2);   // correct
        answers.put(7, 2);   // correct
        answers.put(8, 0);   // wrong
        answers.put(9, 0);   // wrong
        answers.put(10, 0);  // wrong

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(7);
        assertThat(result.score()).isEqualTo(7.0 / 10.0);
    }
}
