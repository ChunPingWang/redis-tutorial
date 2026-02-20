package com.tutorial.redis.module11;

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

@DisplayName("Module 11 Quiz — RediSearch 全文檢索與索引")
class Module11QuizTest {

    private Quiz quiz;

    @BeforeEach
    void setUp() {
        quiz = new Quiz(
                "Module 11 — RediSearch 全文檢索與索引",
                "module-11",
                List.of(
                        new QuizQuestion(1,
                                "FT.CREATE 命令的作用是?",
                                List.of("建立搜尋索引", "建立資料庫", "建立集合", "建立快取"),
                                0,
                                "FT.CREATE 用於在 Redis 中建立一個 RediSearch 全文搜尋索引，定義欄位類型和索引的 Key 前綴。"),
                        new QuizQuestion(2,
                                "RediSearch 支援哪些索引欄位類型?",
                                List.of("TEXT, NUMERIC, TAG, GEO, VECTOR", "只有 TEXT", "STRING, INT, FLOAT", "VARCHAR, INTEGER"),
                                0,
                                "RediSearch 支援多種欄位類型：TEXT (全文搜尋)、NUMERIC (數值範圍)、TAG (精確匹配)、GEO (地理位置)、VECTOR (向量搜尋)。"),
                        new QuizQuestion(3,
                                "FT.SEARCH 中 TAG 欄位的查詢語法是?",
                                List.of("@field:{value}", "@field:value", "field=value", "#field:value"),
                                0,
                                "TAG 欄位使用大括號語法 @field:{value} 進行精確匹配查詢，支援多值查詢如 @field:{val1|val2}。"),
                        new QuizQuestion(4,
                                "FT.SEARCH 中數值範圍查詢的語法是?",
                                List.of("@field:[min max]", "@field:min-max", "@field BETWEEN min AND max", "@field>=min AND @field<=max"),
                                0,
                                "NUMERIC 欄位使用方括號語法 @field:[min max] 進行範圍查詢，支援 +inf 和 -inf 表示無限大/小。"),
                        new QuizQuestion(5,
                                "FT.AGGREGATE 的用途是?",
                                List.of("對搜尋結果進行分組與聚合計算", "合併多個索引", "壓縮索引大小", "建立聚合索引"),
                                0,
                                "FT.AGGREGATE 對搜尋結果進行 GROUPBY 分組和 REDUCE 聚合計算，類似 SQL 的 GROUP BY 和聚合函數。"),
                        new QuizQuestion(6,
                                "FT.SUGADD 命令的用途是?",
                                List.of("新增自動完成建議", "新增搜尋索引", "新增文件", "新增標籤"),
                                0,
                                "FT.SUGADD 將一個建議字串加入自動完成字典中，並指定權重分數，用於實現搜尋框的自動完成功能。"),
                        new QuizQuestion(7,
                                "RediSearch 的反向索引 (Inverted Index) 是什麼?",
                                List.of("從詞彙映射到包含該詞的文件列表", "從文件映射到詞彙列表", "按字母排序的索引", "按時間排序的索引"),
                                0,
                                "反向索引將每個詞彙映射到包含該詞的所有文件列表，使得全文搜尋可以快速找到匹配的文件。"),
                        new QuizQuestion(8,
                                "TEXT 欄位的 WEIGHT 參數作用是?",
                                List.of("調整該欄位在搜尋排名中的權重", "限制欄位的最大長度", "設定欄位的資料類型", "控制索引的壓縮率"),
                                0,
                                "WEIGHT 參數調整 TEXT 欄位在搜尋結果排名計算中的權重，預設為 1.0，數值越高代表該欄位越重要。"),
                        new QuizQuestion(9,
                                "NUMERIC SORTABLE 的意思是?",
                                List.of("數值欄位支援排序操作", "數值欄位自動排序", "只能做排序不能搜尋", "數值欄位可以被刪除"),
                                0,
                                "SORTABLE 表示該 NUMERIC 欄位建立額外的排序索引，使 FT.SEARCH 結果可以按此欄位排序 (SORTBY)。"),
                        new QuizQuestion(10,
                                "FT.SEARCH 預設回傳的最大文件數是?",
                                List.of("10", "100", "1000", "無限制"),
                                0,
                                "FT.SEARCH 預設使用 LIMIT 0 10，即從第 0 筆開始回傳最多 10 筆結果，可透過 LIMIT 參數調整。"),
                        new QuizQuestion(11,
                                "FT.DROPINDEX 會刪除底層的 Hash 資料嗎?",
                                List.of("不會，只刪除索引", "會一起刪除", "視參數而定", "只刪除部分資料"),
                                0,
                                "FT.DROPINDEX 預設只刪除索引結構，不影響底層的 Hash 資料。若加上 DD 參數則會連同資料一起刪除。"),
                        new QuizQuestion(12,
                                "RediSearch 前綴搜尋的語法是?",
                                List.of("wire*", "wire%", "*wire", "?wire"),
                                0,
                                "RediSearch 使用星號 * 作為前綴搜尋的萬用字元，例如 wire* 會匹配 wireless、wired 等以 wire 開頭的詞彙。")
                )
        );
    }

    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        // Arrange — all correct answers at index 0
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // 建立搜尋索引
        answers.put(2, 0);   // TEXT, NUMERIC, TAG, GEO, VECTOR
        answers.put(3, 0);   // @field:{value}
        answers.put(4, 0);   // @field:[min max]
        answers.put(5, 0);   // 對搜尋結果進行分組與聚合計算
        answers.put(6, 0);   // 新增自動完成建議
        answers.put(7, 0);   // 從詞彙映射到包含該詞的文件列表
        answers.put(8, 0);   // 調整該欄位在搜尋排名中的權重
        answers.put(9, 0);   // 數值欄位支援排序操作
        answers.put(10, 0);  // 10
        answers.put(11, 0);  // 不會，只刪除索引
        answers.put(12, 0);  // wire*

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(12);
    }

    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 10/12 正確")
    void quiz_PassesAt80Percent() {
        // Arrange — 10 correct + 2 wrong
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct — 建立搜尋索引
        answers.put(2, 0);   // correct — TEXT, NUMERIC, TAG, GEO, VECTOR
        answers.put(3, 0);   // correct — @field:{value}
        answers.put(4, 0);   // correct — @field:[min max]
        answers.put(5, 0);   // correct — 對搜尋結果進行分組與聚合計算
        answers.put(6, 0);   // correct — 新增自動完成建議
        answers.put(7, 0);   // correct — 從詞彙映射到包含該詞的文件列表
        answers.put(8, 0);   // correct — 調整該欄位在搜尋排名中的權重
        answers.put(9, 0);   // correct — 數值欄位支援排序操作
        answers.put(10, 0);  // correct — 10
        answers.put(11, 1);  // wrong — chose "會一起刪除" instead of "不會，只刪除索引"
        answers.put(12, 1);  // wrong — chose "wire%" instead of "wire*"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(10);
        assertThat(result.score()).isCloseTo(10.0 / 12.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 9/12 正確")
    void quiz_FailsBelow80Percent() {
        // Arrange — 9 correct + 3 wrong (9/12 = 0.75 < 0.8)
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct — 建立搜尋索引
        answers.put(2, 0);   // correct — TEXT, NUMERIC, TAG, GEO, VECTOR
        answers.put(3, 0);   // correct — @field:{value}
        answers.put(4, 0);   // correct — @field:[min max]
        answers.put(5, 0);   // correct — 對搜尋結果進行分組與聚合計算
        answers.put(6, 0);   // correct — 新增自動完成建議
        answers.put(7, 0);   // correct — 從詞彙映射到包含該詞的文件列表
        answers.put(8, 0);   // correct — 調整該欄位在搜尋排名中的權重
        answers.put(9, 0);   // correct — 數值欄位支援排序操作
        answers.put(10, 1);  // wrong — chose "100" instead of "10"
        answers.put(11, 1);  // wrong — chose "會一起刪除" instead of "不會，只刪除索引"
        answers.put(12, 1);  // wrong — chose "wire%" instead of "wire*"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(9);
        assertThat(result.score()).isEqualTo(9.0 / 12.0);
    }
}
