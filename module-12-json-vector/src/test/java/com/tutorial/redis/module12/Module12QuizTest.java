package com.tutorial.redis.module12;

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
 * Module 12 測驗測試類別，驗證 RedisJSON 與 Vector Search 的知識問答。
 * 涵蓋 JSON.SET、JSON.GET、JSON.NUMINCRBY、JSON.ARRAPPEND 等 RedisJSON 指令，
 * 以及 KNN 向量搜尋、FLAT/HNSW 演算法、COSINE 距離度量等概念。
 * 此測試不屬於特定架構層，而是驗證學習成果的測驗機制。
 */
@DisplayName("Module 12 Quiz — RedisJSON + Vector Search")
class Module12QuizTest {

    private Quiz quiz;

    @BeforeEach
    void setUp() {
        quiz = new Quiz(
                "Module 12 — RedisJSON + Vector Search",
                "module-12",
                List.of(
                        new QuizQuestion(1,
                                "JSON.SET 命令的作用是?",
                                List.of("設定 JSON 文件或其中的路徑值", "設定字串值", "建立 Hash", "建立索引"),
                                0,
                                "JSON.SET 用於在 Redis 中設定 JSON 文件或更新文件中特定路徑的值，支援完整的 JSONPath 語法。"),
                        new QuizQuestion(2,
                                "JSON Path 中 '$' 代表什麼?",
                                List.of("JSON 文件的根路徑", "所有子元素", "最後一個元素", "父路徑"),
                                0,
                                "'$' 是 JSONPath 的根路徑符號，代表整個 JSON 文件的起始點。"),
                        new QuizQuestion(3,
                                "JSON.NUMINCRBY 的作用是?",
                                List.of("原子性地遞增 JSON 中的數值欄位", "新增數值欄位", "計算數值總和", "設定數值上限"),
                                0,
                                "JSON.NUMINCRBY 可以原子性地對 JSON 文件中指定路徑的數值進行遞增或遞減操作，無需先讀取再寫入。"),
                        new QuizQuestion(4,
                                "JSON.ARRAPPEND 的作用是?",
                                List.of("在 JSON 陣列末尾新增元素", "建立新陣列", "合併兩個陣列", "取得陣列長度"),
                                0,
                                "JSON.ARRAPPEND 將一個或多個 JSON 元素追加到指定路徑的陣列末尾，是原子操作。"),
                        new QuizQuestion(5,
                                "Vector Search 中 COSINE 距離度量衡量的是?",
                                List.of("兩個向量之間的角度相似度", "歐幾里得距離", "曼哈頓距離", "漢明距離"),
                                0,
                                "COSINE 距離度量計算兩個向量之間的餘弦相似度，衡量方向上的相似程度，不受向量長度影響。"),
                        new QuizQuestion(6,
                                "KNN 搜尋中 K 代表什麼?",
                                List.of("回傳最相似的 K 個結果", "向量維度", "索引數量", "搜尋深度"),
                                0,
                                "KNN (K-Nearest Neighbours) 中的 K 表示要回傳的最近鄰居數量，即最相似的 K 個結果。"),
                        new QuizQuestion(7,
                                "FLAT 向量索引演算法的特性是?",
                                List.of("暴力搜尋，精確但較慢", "近似搜尋，快但不精確", "分層搜尋", "樹狀搜尋"),
                                0,
                                "FLAT 演算法對所有向量進行暴力搜尋 (brute-force)，保證找到精確的最近鄰居，但隨資料量增長效能下降。"),
                        new QuizQuestion(8,
                                "HNSW 向量索引演算法的特性是?",
                                List.of("近似最近鄰搜尋，較快但可能不完全精確", "暴力搜尋", "只支援小資料集", "不支援 COSINE 度量"),
                                0,
                                "HNSW (Hierarchical Navigable Small World) 使用分層圖結構進行近似最近鄰搜尋，速度遠快於 FLAT 但結果可能不完全精確。"),
                        new QuizQuestion(9,
                                "RedisJSON 相較於將 JSON 序列化為 String 的優勢是?",
                                List.of("支援部分更新和路徑查詢，無需讀取整個文件", "佔用更少記憶體", "更快的序列化速度", "自動建立索引"),
                                0,
                                "RedisJSON 原生支援 JSON 結構，可以直接對子路徑進行讀寫操作，無需將整個文件反序列化後再序列化回去。"),
                        new QuizQuestion(10,
                                "Vector Embedding 在 AI 應用中的用途是?",
                                List.of("將文字/圖片等非結構化資料轉換為數值向量以進行相似度比對", "壓縮資料", "加密資料", "建立關聯式資料庫索引"),
                                0,
                                "Vector Embedding 將非結構化資料 (文字、圖片等) 轉換為高維數值向量，使得語義相近的資料在向量空間中距離相近，便於相似度搜尋。")
                )
        );
    }

    // 驗證全部答對時測驗應通過，分數為 100%
    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        // Arrange — all correct answers at index 0
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // 設定 JSON 文件或其中的路徑值
        answers.put(2, 0);   // JSON 文件的根路徑
        answers.put(3, 0);   // 原子性地遞增 JSON 中的數值欄位
        answers.put(4, 0);   // 在 JSON 陣列末尾新增元素
        answers.put(5, 0);   // 兩個向量之間的角度相似度
        answers.put(6, 0);   // 回傳最相似的 K 個結果
        answers.put(7, 0);   // 暴力搜尋，精確但較慢
        answers.put(8, 0);   // 近似最近鄰搜尋，較快但可能不完全精確
        answers.put(9, 0);   // 支援部分更新和路徑查詢，無需讀取整個文件
        answers.put(10, 0);  // 將文字/圖片等非結構化資料轉換為數值向量以進行相似度比對

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(10);
    }

    // 驗證答對 80%（8/10）時測驗仍應通過
    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 8/10 正確")
    void quiz_PassesAt80Percent() {
        // Arrange — 8 correct + 2 wrong
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct — 設定 JSON 文件或其中的路徑值
        answers.put(2, 0);   // correct — JSON 文件的根路徑
        answers.put(3, 0);   // correct — 原子性地遞增 JSON 中的數值欄位
        answers.put(4, 0);   // correct — 在 JSON 陣列末尾新增元素
        answers.put(5, 0);   // correct — 兩個向量之間的角度相似度
        answers.put(6, 0);   // correct — 回傳最相似的 K 個結果
        answers.put(7, 0);   // correct — 暴力搜尋，精確但較慢
        answers.put(8, 0);   // correct — 近似最近鄰搜尋，較快但可能不完全精確
        answers.put(9, 1);   // wrong — chose "佔用更少記憶體" instead of "支援部分更新和路徑查詢"
        answers.put(10, 1);  // wrong — chose "壓縮資料" instead of "將文字/圖片等非結構化資料轉換為數值向量"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(8);
        assertThat(result.score()).isEqualTo(8.0 / 10.0);
    }

    // 驗證答對低於 80%（7/10）時測驗應不通過
    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 7/10 正確")
    void quiz_FailsBelow80Percent() {
        // Arrange — 7 correct + 3 wrong
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct — 設定 JSON 文件或其中的路徑值
        answers.put(2, 0);   // correct — JSON 文件的根路徑
        answers.put(3, 0);   // correct — 原子性地遞增 JSON 中的數值欄位
        answers.put(4, 0);   // correct — 在 JSON 陣列末尾新增元素
        answers.put(5, 0);   // correct — 兩個向量之間的角度相似度
        answers.put(6, 0);   // correct — 回傳最相似的 K 個結果
        answers.put(7, 0);   // correct — 暴力搜尋，精確但較慢
        answers.put(8, 1);   // wrong — chose "暴力搜尋" instead of "近似最近鄰搜尋"
        answers.put(9, 1);   // wrong — chose "佔用更少記憶體" instead of "支援部分更新和路徑查詢"
        answers.put(10, 1);  // wrong — chose "壓縮資料" instead of "將文字/圖片等非結構化資料轉換為數值向量"

        // Act
        QuizResult result = QuizRunner.run(quiz, answers);

        // Assert
        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(7);
        assertThat(result.score()).isEqualTo(7.0 / 10.0);
    }
}
