package com.tutorial.redis.module07;

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

/**
 * 驗證 Module 07 的測驗問答邏輯，涵蓋 Redis Streams 核心觀念。
 * 測驗涵蓋 XADD、XREAD、XREADGROUP、XACK、Consumer Group、
 * Event Sourcing 以及 Pub/Sub 與 Streams 的比較等知識點。
 * 所屬層級：無特定層級（跨領域知識測驗）
 */
@DisplayName("Module 07 Quiz — Redis Streams 與事件驅動")
class Module07QuizTest {

    private static final Quiz QUIZ = new Quiz(
            "Redis Streams 與事件驅動",
            "module-07",
            List.of(
                    new QuizQuestion(1,
                            "Redis Pub/Sub 的最大限制是？",
                            List.of(
                                    "訂閱者離線時消息會遺失",
                                    "不支援多個頻道",
                                    "消息大小有硬性限制",
                                    "不支援模式匹配訂閱"
                            ),
                            0,
                            "Redis Pub/Sub 是 fire-and-forget 模式，訂閱者離線時消息會遺失，因為 Pub/Sub 不提供持久化或重播功能。"),
                    new QuizQuestion(2,
                            "Redis Streams 與 Pub/Sub 最大差異？",
                            List.of(
                                    "Streams 只支援單一消費者",
                                    "Streams 支援持久化與回溯消費",
                                    "Pub/Sub 支援 Consumer Group",
                                    "Pub/Sub 消息可以回溯"
                            ),
                            1,
                            "Redis Streams 支援持久化、消息回溯、Consumer Group 等功能，而 Pub/Sub 是即時的 fire-and-forget 模式，不保存歷史消息。"),
                    new QuizQuestion(3,
                            "XADD 命令的用途是？",
                            List.of(
                                    "建立 Consumer Group",
                                    "新增消息到 Stream",
                                    "讀取 Stream 中的消息",
                                    "確認消息已處理"
                            ),
                            1,
                            "XADD 是 Redis Streams 的基本寫入命令，用於將新的 field-value 對作為一筆新消息新增到 Stream 中。"),
                    new QuizQuestion(4,
                            "Consumer Group 的主要目的是？",
                            List.of(
                                    "限制消費者數量",
                                    "多個消費者分擔處理同一 Stream 的消息",
                                    "加密消息傳輸",
                                    "自動刪除已消費的消息"
                            ),
                            1,
                            "Consumer Group 允許多個消費者協作處理同一 Stream 的消息，每條消息只會被 Group 中的一個消費者處理，實現負載分擔。"),
                    new QuizQuestion(5,
                            "XACK 命令的用途是？",
                            List.of(
                                    "新增消息到 Stream",
                                    "確認消息已被成功處理",
                                    "查詢 pending 消息",
                                    "建立 Consumer Group"
                            ),
                            1,
                            "XACK 用於確認 Consumer Group 中的消息已被成功處理，將消息從 PEL（Pending Entries List）中移除。"),
                    new QuizQuestion(6,
                            "XPENDING 的用途是？",
                            List.of(
                                    "新增 pending 狀態的消息",
                                    "查詢未被確認的 pending 消息",
                                    "刪除 pending 消息",
                                    "將消息標記為 pending"
                            ),
                            1,
                            "XPENDING 用於查詢 Consumer Group 中未被 XACK 確認的 pending 消息，幫助監控消息處理狀態並發現故障消費者。"),
                    new QuizQuestion(7,
                            "XCLAIM 的用途是？",
                            List.of(
                                    "建立新的消費者",
                                    "將長時間未確認的消息轉移給其他消費者",
                                    "刪除消費者",
                                    "暫停消費者的消息接收"
                            ),
                            1,
                            "XCLAIM 用於將長時間未被確認的 pending 消息從一個消費者轉移給另一個消費者，處理消費者故障的情況。"),
                    new QuizQuestion(8,
                            "Event Sourcing 模式中，如何重建狀態？",
                            List.of(
                                    "從快照表讀取最新狀態",
                                    "依序重播所有事件",
                                    "查詢最後一筆事件的狀態欄位",
                                    "從備份資料庫還原"
                            ),
                            1,
                            "Event Sourcing 模式中，狀態是透過依序重播（replay）所有歷史事件來重建的，從初始狀態開始逐一套用每個事件。"),
                    new QuizQuestion(9,
                            "Redis Stream 的 Message ID 格式是？",
                            List.of(
                                    "UUID v4",
                                    "{timestampMs}-{sequenceNumber}",
                                    "自動遞增整數",
                                    "SHA-256 雜湊值"
                            ),
                            1,
                            "Redis Stream 的 Message ID 預設格式為 {millisecondsTime}-{sequenceNumber}，例如 1609459200000-0，由時間戳毫秒數和序列號組成。"),
                    new QuizQuestion(10,
                            "Pub/Sub 與 Streams 何者適合可靠消息處理？",
                            List.of(
                                    "Pub/Sub",
                                    "Streams",
                                    "兩者都適合",
                                    "兩者都不適合"
                            ),
                            1,
                            "Redis Streams 支援持久化、Consumer Group、ACK 確認、Pending 監控等機制，提供 at-least-once 傳遞保證，適合可靠消息處理場景。")
            )
    );

    // 驗證 10 題全部答對時，測驗結果為通過且分數為滿分
    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // 訂閱者離線時消息會遺失
        answers.put(2, 1);   // Streams 支援持久化與回溯消費
        answers.put(3, 1);   // 新增消息到 Stream
        answers.put(4, 1);   // 多個消費者分擔處理同一 Stream 的消息
        answers.put(5, 1);   // 確認消息已被成功處理
        answers.put(6, 1);   // 查詢未被確認的 pending 消息
        answers.put(7, 1);   // 將長時間未確認的消息轉移給其他消費者
        answers.put(8, 1);   // 依序重播所有事件
        answers.put(9, 1);   // {timestampMs}-{sequenceNumber}
        answers.put(10, 1);  // Streams

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(10);
    }

    // 驗證答對 8/10 題（80%）時，測驗結果仍為通過
    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 8/10 正確")
    void quiz_PassesAt80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct
        answers.put(2, 1);   // correct
        answers.put(3, 1);   // correct
        answers.put(4, 1);   // correct
        answers.put(5, 1);   // correct
        answers.put(6, 1);   // correct
        answers.put(7, 1);   // correct
        answers.put(8, 1);   // correct
        answers.put(9, 0);   // wrong
        answers.put(10, 0);  // wrong

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(8);
        assertThat(result.score()).isEqualTo(8.0 / 10.0);
    }

    // 驗證答對 7/10 題（70%）時，低於 80% 門檻，測驗結果為不通過
    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 7/10 正確")
    void quiz_FailsBelow80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct
        answers.put(2, 1);   // correct
        answers.put(3, 1);   // correct
        answers.put(4, 1);   // correct
        answers.put(5, 1);   // correct
        answers.put(6, 1);   // correct
        answers.put(7, 1);   // correct
        answers.put(8, 0);   // wrong
        answers.put(9, 0);   // wrong
        answers.put(10, 0);  // wrong

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(7);
        assertThat(result.score()).isEqualTo(7.0 / 10.0);
    }
}
