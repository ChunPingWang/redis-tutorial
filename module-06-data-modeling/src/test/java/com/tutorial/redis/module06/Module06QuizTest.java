package com.tutorial.redis.module06;

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
 * 測試 Module 06 的隨堂測驗，驗證學習者對 Redis 資料建模概念的理解。
 * 涵蓋 Key 命名慣例、Hash per entity、JSON String、二級索引、Sorted Set 時間索引等核心概念。
 * 屬於模組層級的知識驗證測試。
 */
@DisplayName("Module 06 Quiz — Redis 資料建模模式")
class Module06QuizTest {

    private static final Quiz QUIZ = new Quiz(
            "Redis 資料建模模式",
            "module-06",
            List.of(
                    new QuizQuestion(1,
                            "Redis Key 命名慣例推薦使用什麼分隔符？",
                            List.of(
                                    "底線 `_`",
                                    "冒號 `:`",
                                    "斜線 `/`",
                                    "點號 `.`"
                            ),
                            1,
                            "Redis 社群慣例使用冒號 `:` 作為 Key 名稱的分隔符，例如 `banking:account:123`，提供清晰的命名空間層次結構。"),
                    new QuizQuestion(2,
                            "Hash per entity pattern 的優勢？",
                            List.of(
                                    "儲存空間最小化",
                                    "支援 partial update 只更新單一欄位",
                                    "查詢速度最快",
                                    "支援巢狀結構"
                            ),
                            1,
                            "Hash per entity 模式將每個欄位存為 Hash 的 field，可以使用 HSET/HGET 對單一欄位進行 partial update，無需讀取整個物件。"),
                    new QuizQuestion(3,
                            "JSON String pattern 的優勢？",
                            List.of(
                                    "支援 partial update",
                                    "節省記憶體",
                                    "整體讀取快速，一次 GET 取回完整物件",
                                    "自動建立索引"
                            ),
                            2,
                            "JSON String 模式將整個物件序列化為 JSON 字串存放在單一 Key，一次 GET 即可取回完整物件，整體讀取效率高。"),
                    new QuizQuestion(4,
                            "二級索引 (Secondary Index) 常用哪種 Redis 結構？",
                            List.of(
                                    "String",
                                    "List",
                                    "Set",
                                    "Stream"
                            ),
                            2,
                            "Set 結構天然去重且支援成員查詢，非常適合建立二級索引，例如 `idx:account:currency:USD` 存放所有 USD 帳戶 ID。"),
                    new QuizQuestion(5,
                            "ZRANGEBYSCORE 的用途？",
                            List.of(
                                    "按字典序範圍查詢",
                                    "按分數範圍查詢 Sorted Set 成員",
                                    "按 Key 前綴掃描",
                                    "按插入順序取得元素"
                            ),
                            1,
                            "ZRANGEBYSCORE 根據分數 (score) 範圍查詢 Sorted Set 中的成員，常用於時間範圍查詢等場景。"),
                    new QuizQuestion(6,
                            "建立時間索引最適合的 Redis 結構？",
                            List.of(
                                    "List，依插入順序",
                                    "Hash，field 為時間戳",
                                    "Sorted Set，score 存時間戳",
                                    "String，值為時間戳"
                            ),
                            2,
                            "Sorted Set 以 score 存放時間戳（epoch millis），可用 ZRANGEBYSCORE 進行高效的時間範圍查詢，是建立時間索引的首選結構。"),
                    new QuizQuestion(7,
                            "刪除有二級索引的實體時需要注意什麼？",
                            List.of(
                                    "只需刪除主 Key 即可",
                                    "需要同時清理索引，否則產生孤立索引",
                                    "Redis 會自動清理索引",
                                    "需要先刪除索引再刪除主 Key"
                            ),
                            1,
                            "Redis 不會自動維護二級索引的一致性，刪除實體時必須同時清理相關的索引 Key，否則索引中會殘留已刪除實體的 ID（孤立索引）。"),
                    new QuizQuestion(8,
                            "Redis Key 長度對效能的影響？",
                            List.of(
                                    "完全沒有影響",
                                    "越長越耗記憶體，但應保持可讀性平衡",
                                    "越短越好，應使用縮寫",
                                    "Key 長度有硬性上限 256 bytes"
                            ),
                            1,
                            "較長的 Key 會占用更多記憶體，但過短的 Key 會犧牲可讀性。應在記憶體效率與可讀性之間取得平衡。"),
                    new QuizQuestion(9,
                            "多 Key 組合模式 (Multi-Key) 的適用場景？",
                            List.of(
                                    "需要原子性操作整個 Aggregate",
                                    "需要獨立查詢 Aggregate 的子集欄位",
                                    "資料量極小時使用",
                                    "只讀場景使用"
                            ),
                            1,
                            "Multi-Key 模式將 Aggregate 的不同子集拆分到不同 Key，適用於需要獨立存取或查詢部分欄位的場景，避免每次都讀取完整物件。"),
                    new QuizQuestion(10,
                            "Redis Hash 儲存 Aggregate 時，field 數量超過多少會從 listpack 變 hashtable？",
                            List.of(
                                    "64",
                                    "128",
                                    "256",
                                    "512"
                            ),
                            1,
                            "Redis Hash 的內部編碼預設在 field 數量超過 128（hash-max-listpack-entries）時，從記憶體高效的 listpack 轉換為 hashtable 編碼。")
            )
    );

    // 驗證所有題目全數答對時，測驗結果為通過且分數為滿分
    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 1);   // 冒號 `:`
        answers.put(2, 1);   // 支援 partial update 只更新單一欄位
        answers.put(3, 2);   // 整體讀取快速，一次 GET 取回完整物件
        answers.put(4, 2);   // Set
        answers.put(5, 1);   // 按分數範圍查詢 Sorted Set 成員
        answers.put(6, 2);   // Sorted Set，score 存時間戳
        answers.put(7, 1);   // 需要同時清理索引，否則產生孤立索引
        answers.put(8, 1);   // 越長越耗記憶體，但應保持可讀性平衡
        answers.put(9, 1);   // 需要獨立查詢 Aggregate 的子集欄位
        answers.put(10, 1);  // 128

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(10);
    }

    // 驗證答對 80%（8/10）時，測驗結果仍為通過（門檻為 80%）
    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 8/10 正確")
    void quiz_PassesAt80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 1);   // correct
        answers.put(2, 1);   // correct
        answers.put(3, 2);   // correct
        answers.put(4, 2);   // correct
        answers.put(5, 1);   // correct
        answers.put(6, 2);   // correct
        answers.put(7, 1);   // correct
        answers.put(8, 1);   // correct
        answers.put(9, 0);   // wrong
        answers.put(10, 0);  // wrong

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(8);
        assertThat(result.score()).isEqualTo(8.0 / 10.0);
    }

    // 驗證答對低於 80%（7/10）時，測驗結果為不通過
    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 7/10 正確")
    void quiz_FailsBelow80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 1);   // correct
        answers.put(2, 1);   // correct
        answers.put(3, 2);   // correct
        answers.put(4, 2);   // correct
        answers.put(5, 1);   // correct
        answers.put(6, 2);   // correct
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
