package com.tutorial.redis.module09;

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

@DisplayName("Module 09 Quiz — Redis 高可用架構")
class Module09QuizTest {

    private Quiz quiz;

    @BeforeEach
    void setUp() {
        quiz = new Quiz(
                "Module 09 — Redis 高可用架構",
                "module-09",
                List.of(
                        new QuizQuestion(1,
                                "Redis 主從複製是什麼模式?",
                                List.of("異步複製 (Asynchronous)", "同步複製 (Synchronous)", "半同步複製", "無複製"),
                                0,
                                "Redis 主從複製預設為異步複製，Master 不等待 Replica 確認即回應客戶端，因此可能有少量資料遺失。"),
                        new QuizQuestion(2,
                                "Sentinel 需要幾個節點才能進行客觀下線 (ODOWN) 投票?",
                                List.of("由 quorum 設定決定", "固定 3 個", "固定 2 個", "1 個即可"),
                                0,
                                "ODOWN 的判定需要達到 quorum 設定的 Sentinel 數量同意，quorum 值在 sentinel.conf 中配置。"),
                        new QuizQuestion(3,
                                "SDOWN 和 ODOWN 的差別是?",
                                List.of("SDOWN 為單一 Sentinel 判斷，ODOWN 為多數決", "SDOWN 比 ODOWN 嚴重", "兩者相同", "ODOWN 由 Master 發起"),
                                0,
                                "SDOWN (Subjectively Down) 是單一 Sentinel 的主觀判斷，ODOWN (Objectively Down) 是多個 Sentinel 達成共識的客觀判斷。"),
                        new QuizQuestion(4,
                                "ReadFrom.REPLICA_PREFERRED 的行為是?",
                                List.of("優先讀 Replica，Replica 不可用時讀 Master", "只讀 Replica", "只讀 Master", "隨機讀取"),
                                0,
                                "REPLICA_PREFERRED 策略優先從 Replica 讀取，當所有 Replica 不可用時退回到 Master 讀取。"),
                        new QuizQuestion(5,
                                "解決 Replication Lag 導致讀到舊值的方法是?",
                                List.of("以上皆是", "Read-Your-Writes 模式", "WAIT 命令", "版本號比對"),
                                0,
                                "解決 Replication Lag 的方法包括 Read-Your-Writes 模式、WAIT 命令、版本號比對等多種手段，可根據場景組合使用。"),
                        new QuizQuestion(6,
                                "WAIT 命令的作用是?",
                                List.of("等待指定數量的 Replica 確認寫入", "等待 Master 重啟", "等待 Sentinel 選舉", "等待 Client 連線"),
                                0,
                                "WAIT numreplicas timeout 命令會阻塞直到指定數量的 Replica 確認已接收先前的寫入命令。"),
                        new QuizQuestion(7,
                                "Sentinel 故障轉移選舉新 Master 的依據包括?",
                                List.of("replica-priority、replication offset、run ID", "隨機選擇", "IP 地址排序", "啟動時間"),
                                0,
                                "Sentinel 選舉新 Master 時依序考慮 replica-priority（越低越優先）、replication offset（越大越新）、run ID（字典序最小）。"),
                        new QuizQuestion(8,
                                "REPLICAOF NO ONE 的作用是?",
                                List.of("將 Replica 升級為獨立的 Master", "刪除所有 Replica", "停止 Redis 服務", "重置複製偏移量"),
                                0,
                                "REPLICAOF NO ONE 命令讓一個 Replica 停止複製並升級為獨立的 Master，可以接受寫入操作。"),
                        new QuizQuestion(9,
                                "Redis Sentinel 通常部署幾個節點?",
                                List.of("至少 3 個（奇數）", "1 個", "2 個", "必須 5 個"),
                                0,
                                "Sentinel 建議部署至少 3 個節點（奇數），以確保多數決投票機制正常運作，避免腦裂問題。"),
                        new QuizQuestion(10,
                                "Replication Backlog 的用途是?",
                                List.of("儲存最近寫入命令以支援部分重同步", "備份 RDB 檔案", "記錄慢查詢", "儲存客戶端連線資訊"),
                                0,
                                "Replication Backlog 是 Master 上的環形緩衝區，儲存最近的寫入命令，讓斷線重連的 Replica 進行部分重同步 (PSYNC) 而非全量同步。")
                )
        );
    }

    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        // Arrange — all correct answers at index 0
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // 異步複製 (Asynchronous)
        answers.put(2, 0);   // 由 quorum 設定決定
        answers.put(3, 0);   // SDOWN 為單一 Sentinel 判斷，ODOWN 為多數決
        answers.put(4, 0);   // 優先讀 Replica，Replica 不可用時讀 Master
        answers.put(5, 0);   // 以上皆是
        answers.put(6, 0);   // 等待指定數量的 Replica 確認寫入
        answers.put(7, 0);   // replica-priority、replication offset、run ID
        answers.put(8, 0);   // 將 Replica 升級為獨立的 Master
        answers.put(9, 0);   // 至少 3 個（奇數）
        answers.put(10, 0);  // 儲存最近寫入命令以支援部分重同步

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(10);
    }

    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 8/10 正確")
    void quiz_PassesAt80Percent() {
        // Arrange — 8 correct + 2 wrong
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct — 異步複製
        answers.put(2, 0);   // correct — 由 quorum 設定決定
        answers.put(3, 0);   // correct — SDOWN 為單一 Sentinel 判斷，ODOWN 為多數決
        answers.put(4, 0);   // correct — 優先讀 Replica
        answers.put(5, 0);   // correct — 以上皆是
        answers.put(6, 0);   // correct — 等待指定數量的 Replica 確認寫入
        answers.put(7, 0);   // correct — replica-priority、replication offset、run ID
        answers.put(8, 0);   // correct — 將 Replica 升級為獨立的 Master
        answers.put(9, 1);   // wrong — chose "1 個" instead of "至少 3 個（奇數）"
        answers.put(10, 1);  // wrong — chose "備份 RDB 檔案" instead of "儲存最近寫入命令以支援部分重同步"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(8);
        assertThat(result.score()).isEqualTo(8.0 / 10.0);
    }

    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 7/10 正確")
    void quiz_FailsBelow80Percent() {
        // Arrange — 7 correct + 3 wrong
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct — 異步複製
        answers.put(2, 0);   // correct — 由 quorum 設定決定
        answers.put(3, 0);   // correct — SDOWN 為單一 Sentinel 判斷，ODOWN 為多數決
        answers.put(4, 0);   // correct — 優先讀 Replica
        answers.put(5, 0);   // correct — 以上皆是
        answers.put(6, 0);   // correct — 等待指定數量的 Replica 確認寫入
        answers.put(7, 0);   // correct — replica-priority、replication offset、run ID
        answers.put(8, 1);   // wrong — chose "刪除所有 Replica" instead of "將 Replica 升級為獨立的 Master"
        answers.put(9, 1);   // wrong — chose "1 個" instead of "至少 3 個（奇數）"
        answers.put(10, 1);  // wrong — chose "備份 RDB 檔案" instead of "儲存最近寫入命令以支援部分重同步"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(7);
        assertThat(result.score()).isEqualTo(7.0 / 10.0);
    }
}
