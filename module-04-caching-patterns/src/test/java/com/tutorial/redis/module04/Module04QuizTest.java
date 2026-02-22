package com.tutorial.redis.module04;

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
 * Module 04 測驗測試類別。
 * 驗證學習者對 Redis 快取模式（Cache-Aside、Read-Through、Write-Behind、TTL 策略、Cache Stampede Protection）的理解。
 * 涵蓋快取穿透、快取雪崩、快取擊穿等核心概念的問答驗證。
 * 屬於模組層級的整合測驗。
 */
@DisplayName("Module 04 Quiz — Redis 快取模式")
class Module04QuizTest {

    private static final Quiz QUIZ = new Quiz(
            "Redis 快取模式",
            "module-04",
            List.of(
                    new QuizQuestion(1,
                            "Cache-Aside 模式的流程是什麼？",
                            List.of(
                                    "App→Cache Miss→DB→Write Cache→Return",
                                    "App→DB→Write Cache→Return",
                                    "App→Cache→DB→Return",
                                    "App→Write Cache→DB→Return"
                            ),
                            0,
                            "Cache-Aside（旁路快取）模式中，應用程式先查快取，未命中時查資料庫，再將結果寫入快取後回傳。"),
                    new QuizQuestion(2,
                            "Write-Behind 模式的主要優勢是什麼？",
                            List.of(
                                    "強一致性保證",
                                    "減少 DB 寫入壓力",
                                    "自動快取失效",
                                    "即時持久化"
                            ),
                            1,
                            "Write-Behind（寫回）模式先寫入緩衝區，再批次寫入資料庫，減少 DB 寫入壓力並提高吞吐量。"),
                    new QuizQuestion(3,
                            "什麼是快取穿透（Cache Penetration）？",
                            List.of(
                                    "快取伺服器宕機導致流量全部打到 DB",
                                    "熱點 Key 過期瞬間大量請求湧入",
                                    "查詢不存在的 Key 繞過快取直接打 DB",
                                    "大量 Key 同時過期導致 DB 壓力暴增"
                            ),
                            2,
                            "快取穿透是指查詢快取和資料庫都不存在的資料，每次請求都會繞過快取直接打到資料庫。"),
                    new QuizQuestion(4,
                            "快取雪崩（Cache Avalanche）的防護方案是什麼？",
                            List.of(
                                    "布隆過濾器",
                                    "分散式鎖",
                                    "空值快取",
                                    "TTL 隨機化"
                            ),
                            3,
                            "快取雪崩是大量 Key 同時過期，導致資料庫壓力暴增。透過 TTL 隨機化可以分散過期時間，避免同時失效。"),
                    new QuizQuestion(5,
                            "快取擊穿（Cache Breakdown）的防護方案是什麼？",
                            List.of(
                                    "TTL 隨機化",
                                    "空值快取",
                                    "分散式鎖保護重建",
                                    "多級快取"
                            ),
                            2,
                            "快取擊穿是單一熱點 Key 過期時大量並發請求同時重建快取。使用分散式鎖可以確保只有一個請求重建，其他請求等待。"),
                    new QuizQuestion(6,
                            "Refresh-Ahead 模式何時觸發刷新？",
                            List.of(
                                    "每次讀取時都刷新",
                                    "TTL 到期時刷新",
                                    "TTL 剩餘不足 20%",
                                    "TTL 剩餘不足 50%"
                            ),
                            2,
                            "Refresh-Ahead 模式在快取命中且 TTL 剩餘不足 20% 時，非同步地提前刷新快取，避免過期後的快取未命中。"),
                    new QuizQuestion(7,
                            "Read-Through 與 Cache-Aside 的主要差異是什麼？",
                            List.of(
                                    "Read-Through 不使用快取",
                                    "Cache-Aside 由快取層自動載入",
                                    "Read-Through 由快取層自動載入",
                                    "Read-Through 只支援寫入操作"
                            ),
                            2,
                            "Read-Through 模式中，快取層負責自動載入資料（對應用透明），而 Cache-Aside 由應用程式自行管理快取的讀寫。"),
                    new QuizQuestion(8,
                            "Write-Through 模式的一致性特點是什麼？",
                            List.of(
                                    "最終一致性且低延遲",
                                    "強一致性但寫入延遲較高",
                                    "弱一致性且高吞吐量",
                                    "無一致性保證"
                            ),
                            1,
                            "Write-Through 同步寫入快取和資料庫，確保強一致性，但因為需要等待兩次寫入完成，寫入延遲較高。"),
                    new QuizQuestion(9,
                            "Multi-Level Cache 中 L1 通常使用什麼？",
                            List.of(
                                    "Redis Cluster",
                                    "Memcached",
                                    "Caffeine/本地記憶體",
                                    "資料庫查詢快取"
                            ),
                            2,
                            "多級快取架構中，L1 使用 Caffeine 等本地記憶體快取（低延遲），L2 使用 Redis（分散式共享），達到效能與一致性的平衡。"),
                    new QuizQuestion(10,
                            "SETNX 在快取場景的主要用途是什麼？",
                            List.of(
                                    "設定快取過期時間",
                                    "分散式鎖保護",
                                    "原子性計數器",
                                    "批次寫入快取"
                            ),
                            1,
                            "SETNX（SET if Not eXists）用於實現分散式鎖，確保只有一個客戶端能成功設定鍵值，常用於快取擊穿保護。")
            )
    );

    // 驗證全部答對時測驗通過且分數為滿分
    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // App→Cache Miss→DB→Write Cache→Return
        answers.put(2, 1);   // 減少 DB 寫入壓力
        answers.put(3, 2);   // 查詢不存在的 Key 繞過快取直接打 DB
        answers.put(4, 3);   // TTL 隨機化
        answers.put(5, 2);   // 分散式鎖保護重建
        answers.put(6, 2);   // TTL 剩餘不足 20%
        answers.put(7, 2);   // Read-Through 由快取層自動載入
        answers.put(8, 1);   // 強一致性但寫入延遲較高
        answers.put(9, 2);   // Caffeine/本地記憶體
        answers.put(10, 1);  // 分散式鎖保護

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(10);
    }

    // 驗證答對 80%（8/10）時測驗仍然通過
    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 8/10 正確")
    void quiz_PassesAt80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct
        answers.put(2, 1);   // correct
        answers.put(3, 2);   // correct
        answers.put(4, 3);   // correct
        answers.put(5, 2);   // correct
        answers.put(6, 2);   // correct
        answers.put(7, 2);   // correct
        answers.put(8, 1);   // correct
        answers.put(9, 0);   // wrong
        answers.put(10, 0);  // wrong

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(8);
        assertThat(result.score()).isEqualTo(8.0 / 10.0);
    }

    // 驗證答對低於 80%（7/10）時測驗不通過
    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 7/10 正確")
    void quiz_FailsBelow80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct
        answers.put(2, 1);   // correct
        answers.put(3, 2);   // correct
        answers.put(4, 3);   // correct
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
