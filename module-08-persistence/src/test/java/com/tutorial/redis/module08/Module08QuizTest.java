package com.tutorial.redis.module08;

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
 * 測試 Module 08 的隨堂測驗，驗證學員對 Redis 持久化概念的理解。
 * 涵蓋 RDB 快照、AOF 日誌、Hybrid 混合持久化及 RPO/RTO 相關知識。
 * 屬於模組層級的整合測驗測試。
 */
@DisplayName("Module 08 Quiz — Redis 持久化策略")
class Module08QuizTest {

    private Quiz quiz;

    @BeforeEach
    void setUp() {
        quiz = new Quiz(
                "Module 08 — Redis 持久化策略",
                "module-08",
                List.of(
                        new QuizQuestion(1,
                                "Redis RDB 持久化機制使用什麼系統呼叫來建立子程序?",
                                List.of("fork()", "exec()", "clone()", "spawn()"),
                                0,
                                "RDB 持久化使用 fork() 系統呼叫建立子程序，子程序負責將記憶體快照寫入磁碟，父程序繼續處理客戶端請求。"),
                        new QuizQuestion(2,
                                "AOF 的 appendfsync 設定為 'always' 代表什麼?",
                                List.of("每筆寫入都 fsync", "每秒 fsync 一次", "由 OS 決定", "不做 fsync"),
                                0,
                                "appendfsync always 表示每筆寫入操作都會觸發 fsync，確保資料即時寫入磁碟，提供最高的資料安全性但效能開銷最大。"),
                        new QuizQuestion(3,
                                "Hybrid 持久化 (aof-use-rdb-preamble) 的優點是?",
                                List.of("結合 RDB 快速載入與 AOF 低資料遺失", "完全不遺失資料", "不需要磁碟空間", "自動壓縮"),
                                0,
                                "Hybrid 持久化結合了 RDB 的快速載入優勢和 AOF 的低資料遺失特性，AOF 檔案前段為 RDB 格式，後段為增量 AOF 日誌。"),
                        new QuizQuestion(4,
                                "RDB 持久化的 RPO (Recovery Point Objective) 通常是?",
                                List.of("分鐘級", "零", "毫秒級", "小時級"),
                                0,
                                "RDB 是週期性快照，兩次快照之間的資料可能遺失，因此 RPO 通常為分鐘級，取決於快照間隔設定。"),
                        new QuizQuestion(5,
                                "BGSAVE 命令的作用是?",
                                List.of("背景執行 RDB 快照", "背景執行 AOF 重寫", "同步儲存 RDB", "清除快照檔案"),
                                0,
                                "BGSAVE 命令觸發 Redis 在背景 fork 子程序執行 RDB 快照，不阻塞主程序的客戶端請求處理。"),
                        new QuizQuestion(6,
                                "AOF Rewrite 的目的是?",
                                List.of("壓縮 AOF 檔案大小", "提高寫入效能", "加密 AOF 檔案", "轉換為 RDB 格式"),
                                0,
                                "AOF Rewrite 透過重建最小化的命令集來壓縮 AOF 檔案大小，移除冗餘的中間操作，只保留最終狀態所需的命令。"),
                        new QuizQuestion(7,
                                "Redis 重啟時，若同時存在 RDB 和 AOF 檔案，預設載入哪個?",
                                List.of("AOF (資料較完整)", "RDB (載入較快)", "兩者合併", "由配置決定"),
                                0,
                                "當 AOF 啟用時，Redis 重啟預設載入 AOF 檔案，因為 AOF 通常包含更完整的資料（較低的資料遺失）。"),
                        new QuizQuestion(8,
                                "Copy-on-Write 機制在 RDB 持久化中的作用是?",
                                List.of("避免 fork 後複製全部記憶體", "加速磁碟寫入", "壓縮資料", "加密快照"),
                                0,
                                "Copy-on-Write (COW) 讓 fork 出的子程序與父程序共享記憶體頁面，只在父程序修改資料時才複製對應的頁面，大幅減少 fork 時的記憶體開銷。")
                )
        );
    }

    // 驗證全部答對時，測驗結果為通過且分數為滿分
    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        // Arrange — all correct answers at index 0
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);  // fork()
        answers.put(2, 0);  // 每筆寫入都 fsync
        answers.put(3, 0);  // 結合 RDB 快速載入與 AOF 低資料遺失
        answers.put(4, 0);  // 分鐘級
        answers.put(5, 0);  // 背景執行 RDB 快照
        answers.put(6, 0);  // 壓縮 AOF 檔案大小
        answers.put(7, 0);  // AOF (資料較完整)
        answers.put(8, 0);  // 避免 fork 後複製全部記憶體

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(8);
    }

    // 驗證答對 7/8 題（87.5%）時，仍可達到 80% 通過門檻
    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 7/8 正確")
    void quiz_PassesAt80Percent() {
        // Arrange — 7 correct + 1 wrong
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);  // correct
        answers.put(2, 0);  // correct
        answers.put(3, 0);  // correct
        answers.put(4, 0);  // correct
        answers.put(5, 0);  // correct
        answers.put(6, 0);  // correct
        answers.put(7, 0);  // correct
        answers.put(8, 1);  // wrong — chose "加速磁碟寫入" instead of "避免 fork 後複製全部記憶體"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(7);
        assertThat(result.score()).isEqualTo(7.0 / 8.0);
    }

    // 驗證答對 6/8 題（75%）時，未達 80% 門檻應判定為不通過
    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 6/8 正確")
    void quiz_FailsBelow80Percent() {
        // Arrange — 6 correct + 2 wrong
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);  // correct
        answers.put(2, 0);  // correct
        answers.put(3, 0);  // correct
        answers.put(4, 0);  // correct
        answers.put(5, 0);  // correct
        answers.put(6, 0);  // correct
        answers.put(7, 1);  // wrong — chose "RDB (載入較快)"
        answers.put(8, 1);  // wrong — chose "加速磁碟寫入"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(6);
        assertThat(result.score()).isEqualTo(6.0 / 8.0);
    }
}
