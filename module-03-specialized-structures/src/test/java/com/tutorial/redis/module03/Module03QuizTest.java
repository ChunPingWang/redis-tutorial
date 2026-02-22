package com.tutorial.redis.module03;

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
 * Module 03 測驗測試類別
 * 驗證學員對 Redis 進階資料結構的理解，涵蓋 Geo、Bitmap、HyperLogLog、
 * Bloom Filter、Cuckoo Filter 與 TimeSeries 等特殊結構的觀念題
 * 屬於模組層級的整合測驗，需達到 80% 以上正確率方可通過
 */
@DisplayName("Module 03 Quiz — Redis 進階資料結構")
class Module03QuizTest {

    private static final Quiz QUIZ = new Quiz(
            "Redis 進階資料結構",
            "module-03",
            List.of(
                    new QuizQuestion(1,
                            "Geospatial 底層基於哪種資料結構？",
                            List.of(
                                    "Sorted Set + Geohash",
                                    "Hash + B-Tree",
                                    "List + R-Tree",
                                    "Set + Quadtree"
                            ),
                            0,
                            "Geospatial 底層使用 Sorted Set 搭配 Geohash 編碼，將經緯度轉換為 52-bit 整數作為 score。"),
                    new QuizQuestion(2,
                            "GEOSEARCH 命令取代了哪個已廢棄命令？",
                            List.of(
                                    "GEOPOS",
                                    "GEODIST",
                                    "GEORADIUS",
                                    "GEOHASH"
                            ),
                            2,
                            "GEOSEARCH 是 Redis 6.2 引入的命令，取代了已廢棄的 GEORADIUS 和 GEORADIUSBYMEMBER。"),
                    new QuizQuestion(3,
                            "Bitmap BITCOUNT 回傳的是什麼？",
                            List.of(
                                    "值為 0 的 bit 數量",
                                    "值為 1 的 bit 數量",
                                    "全部 bit 的總數量",
                                    "最後一個 1 的位置"
                            ),
                            1,
                            "BITCOUNT 回傳字串中值為 1 的 bit 數量，常用於計算活躍天數等場景。"),
                    new QuizQuestion(4,
                            "HyperLogLog 的標準誤差率是多少？",
                            List.of(
                                    "0.01%",
                                    "0.1%",
                                    "0.81%",
                                    "1.5%"
                            ),
                            2,
                            "HyperLogLog 的標準誤差率為 0.81%，這是由其使用的 16384 個暫存器決定的。"),
                    new QuizQuestion(5,
                            "HyperLogLog 固定佔用多少記憶體？",
                            List.of(
                                    "1 KB",
                                    "4 KB",
                                    "12 KB",
                                    "64 KB"
                            ),
                            2,
                            "HyperLogLog 固定佔用 12 KB 記憶體（16384 個 6-bit 暫存器），無論基數大小。"),
                    new QuizQuestion(6,
                            "Bloom Filter 的特性是什麼？",
                            List.of(
                                    "可能有假陰性，但不會有假陽性",
                                    "可能有假陽性，但不會有假陰性",
                                    "既不會有假陽性也不會有假陰性",
                                    "假陽性和假陰性都可能發生"
                            ),
                            1,
                            "Bloom Filter 可能有假陽性（false positive），但不會有假陰性（false negative），即「可能存在」或「絕對不存在」。"),
                    new QuizQuestion(7,
                            "Cuckoo Filter 相比 Bloom Filter 的優勢是什麼？",
                            List.of(
                                    "更低的假陽性率",
                                    "更少的記憶體使用",
                                    "支援刪除操作",
                                    "更快的查詢速度"
                            ),
                            2,
                            "Cuckoo Filter 支援刪除操作（CF.DEL），這是 Bloom Filter 無法做到的，因為 Bloom Filter 的多個 hash 函數會影響其他元素。"),
                    new QuizQuestion(8,
                            "TS.ADD 命令中使用 * 作為 timestamp 的意義是什麼？",
                            List.of(
                                    "使用客戶端當前時間",
                                    "使用 Redis 伺服器當前時間",
                                    "自動遞增上一個 timestamp",
                                    "使用 Unix epoch 起始時間"
                            ),
                            1,
                            "TS.ADD 中的 * 表示使用 Redis 伺服器的當前時間作為 timestamp，確保時間一致性。"),
                    new QuizQuestion(9,
                            "使用 PFMERGE 可以做什麼？",
                            List.of(
                                    "合併多個 Set 計算交集",
                                    "合併多個 HyperLogLog 計算聯集基數",
                                    "合併多個 Sorted Set 計算總分",
                                    "合併多個 Bloom Filter"
                            ),
                            1,
                            "PFMERGE 將多個 HyperLogLog 合併為一個，用於計算聯集的估計基數，例如合併每日 UV 計算週 UV。"),
                    new QuizQuestion(10,
                            "Bloom Filter BF.RESERVE 的 error_rate 參數代表什麼？",
                            List.of(
                                    "假陽性機率",
                                    "假陰性機率",
                                    "資料遺失機率",
                                    "記憶體溢出機率"
                            ),
                            0,
                            "BF.RESERVE 的 error_rate 參數代表假陽性機率（false positive probability），值越小過濾器越精確但佔用更多記憶體。")
            )
    );

    // 驗證全部 10 題答對時，測驗結果為通過且分數為 1.0
    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // Sorted Set + Geohash
        answers.put(2, 2);   // GEORADIUS
        answers.put(3, 1);   // 值為 1 的 bit 數量
        answers.put(4, 2);   // 0.81%
        answers.put(5, 2);   // 12 KB
        answers.put(6, 1);   // 可能有假陽性，但不會有假陰性
        answers.put(7, 2);   // 支援刪除操作
        answers.put(8, 1);   // 使用 Redis 伺服器當前時間
        answers.put(9, 1);   // 合併多個 HyperLogLog 計算聯集基數
        answers.put(10, 0);  // 假陽性機率

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(10);
    }

    // 驗證答對 8 題（80%）時仍可通過測驗門檻
    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 8/10 正確")
    void quiz_PassesAt80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct
        answers.put(2, 2);   // correct
        answers.put(3, 1);   // correct
        answers.put(4, 2);   // correct
        answers.put(5, 2);   // correct
        answers.put(6, 1);   // correct
        answers.put(7, 2);   // correct
        answers.put(8, 1);   // correct
        answers.put(9, 0);   // wrong
        answers.put(10, 1);  // wrong

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(8);
        assertThat(result.score()).isEqualTo(8.0 / 10.0);
    }

    // 驗證答對 7 題（70%）時低於 80% 門檻，測驗不通過
    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 7/10 正確")
    void quiz_FailsBelow80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct
        answers.put(2, 2);   // correct
        answers.put(3, 1);   // correct
        answers.put(4, 2);   // correct
        answers.put(5, 2);   // correct
        answers.put(6, 1);   // correct
        answers.put(7, 2);   // correct
        answers.put(8, 0);   // wrong
        answers.put(9, 0);   // wrong
        answers.put(10, 1);  // wrong

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(7);
        assertThat(result.score()).isEqualTo(7.0 / 10.0);
    }
}
