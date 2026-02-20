package com.tutorial.redis.module02;

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

@DisplayName("Module 02 Quiz — Redis 核心資料結構")
class Module02QuizTest {

    private static final Quiz QUIZ = new Quiz(
            "Redis 核心資料結構",
            "module-02",
            List.of(
                    new QuizQuestion(1,
                            "String 內部編碼（int/embstr/raw）的選擇條件是？",
                            List.of(
                                    "int 用於可解析為 64-bit 整數的值",
                                    "embstr 用於所有字串值",
                                    "raw 用於長度小於 44 bytes 的字串",
                                    "int 用於所有數值型態"
                            ),
                            0,
                            "int 用於可解析為 64-bit 整數的值；embstr 用於 <= 44 bytes 的字串；raw 用於 > 44 bytes 的字串。"),
                    new QuizQuestion(2,
                            "INCR 命令的時間複雜度是？",
                            List.of("O(N)", "O(log N)", "O(1)", "O(N log N)"),
                            2,
                            "INCR 是原子操作，時間複雜度為 O(1)。"),
                    new QuizQuestion(3,
                            "Hash 內部編碼從 listpack 切換到 hashtable 的閾值條件是？",
                            List.of(
                                    "field 數量超過 64 或 field value 超過 128 bytes",
                                    "field 數量超過 128 或 field value 超過 64 bytes",
                                    "field 數量超過 256 或 field value 超過 32 bytes",
                                    "field 數量超過 512"
                            ),
                            1,
                            "field 數量超過 128 或 field value 超過 64 bytes 時，Hash 從 listpack 切換到 hashtable。"),
                    new QuizQuestion(4,
                            "購物車為何選擇 Hash 而非 String（JSON）來實作？",
                            List.of(
                                    "Hash 佔用更少記憶體",
                                    "Hash 支援 partial update，不需讀取整個物件",
                                    "Hash 的讀取速度更快",
                                    "Hash 支援 TTL 設定"
                            ),
                            1,
                            "Hash 支援 partial update，可針對單一 field 做 HSET/HDEL，不需反序列化整個 JSON 物件。"),
                    new QuizQuestion(5,
                            "List 中 LPUSH + LTRIM 組合的用途是？",
                            List.of(
                                    "實現 FIFO Queue",
                                    "實現 Priority Queue",
                                    "Capped Collection — 維護固定大小的最近記錄",
                                    "實現 Stack"
                            ),
                            2,
                            "LPUSH 加入新元素到頭部，LTRIM 裁剪尾部舊元素，形成固定大小的 Capped Collection。"),
                    new QuizQuestion(6,
                            "LRANGE 的時間複雜度是？",
                            List.of(
                                    "O(1)",
                                    "O(N)，N 是 List 長度",
                                    "O(S+N)，S 是起始偏移量，N 是返回元素數",
                                    "O(log N)"
                            ),
                            2,
                            "LRANGE 的時間複雜度為 O(S+N)，S 是起始偏移量，N 是返回元素數。"),
                    new QuizQuestion(7,
                            "Set SINTER 的時間複雜度是？",
                            List.of(
                                    "O(1)",
                                    "O(N)，N 是最小集合大小",
                                    "O(N*M)，N 是最小集合大小，M 是集合數量",
                                    "O(N log N)"
                            ),
                            2,
                            "SINTER 的時間複雜度為 O(N*M)，N 是最小集合的元素數，M 是參與交集的集合數量。"),
                    new QuizQuestion(8,
                            "Sorted Set 的底層資料結構是？",
                            List.of(
                                    "B+ Tree",
                                    "Red-Black Tree",
                                    "skip list + hash table",
                                    "Trie"
                            ),
                            2,
                            "Sorted Set 使用 skip list（跳躍表）支援有序操作，搭配 hash table 實現 O(1) 的成員查詢。"),
                    new QuizQuestion(9,
                            "ZADD NX 與 XX 的差異是？",
                            List.of(
                                    "NX 只新增不存在的 member，XX 只更新已存在的 member",
                                    "NX 不設定過期，XX 設定過期",
                                    "NX 只能新增一個，XX 能新增多個",
                                    "NX 使用 float score，XX 使用 int score"
                            ),
                            0,
                            "NX 只新增不存在的 member（不更新已存在的），XX 只更新已存在的 member（不新增）。"),
                    new QuizQuestion(10,
                            "哪種 Redis 資料結構最適合實作排行榜？",
                            List.of(
                                    "String",
                                    "Hash",
                                    "List",
                                    "Sorted Set"
                            ),
                            3,
                            "Sorted Set 天然支援分數排序，ZREVRANGE 可取得排名，適合排行榜場景。"),
                    new QuizQuestion(11,
                            "Hash HGETALL 的風險是什麼？",
                            List.of(
                                    "可能導致記憶體溢出",
                                    "O(N) 時間複雜度，大 Hash 會阻塞 Redis",
                                    "不支援大於 1MB 的 Hash",
                                    "會自動刪除 Hash"
                            ),
                            1,
                            "HGETALL 是 O(N) 操作，當 Hash 包含大量 field 時會阻塞 Redis 單執行緒。"),
                    new QuizQuestion(12,
                            "String MGET/MSET 的優勢是？",
                            List.of(
                                    "支援事務",
                                    "支援 pipeline",
                                    "減少 RTT，一次 round trip 處理多個 key",
                                    "自動設定 TTL"
                            ),
                            2,
                            "MGET/MSET 在一次 round trip 中處理多個 key，大幅減少網路 RTT 延遲。"),
                    new QuizQuestion(13,
                            "Set 與 Sorted Set 的選擇原則是？",
                            List.of(
                                    "資料量大用 Set，資料量小用 Sorted Set",
                                    "不需要排序用 Set，需要分數排序用 Sorted Set",
                                    "只存字串用 Set，存數字用 Sorted Set",
                                    "單一操作用 Set，批次操作用 Sorted Set"
                            ),
                            1,
                            "Set 是無序唯一集合，Sorted Set 則為每個元素附帶分數並自動排序；選擇依據是否需要排序。"),
                    new QuizQuestion(14,
                            "List 的 quicklist 內部結構是由什麼組成的？",
                            List.of(
                                    "陣列（array）",
                                    "紅黑樹節點",
                                    "由多個 listpack 節點組成的雙向鏈表",
                                    "B-Tree 節點"
                            ),
                            2,
                            "quicklist 是由多個 listpack（壓縮列表）節點組成的雙向鏈表，兼顧記憶體效率與操作效能。"),
                    new QuizQuestion(15,
                            "OBJECT ENCODING 命令的用途是？",
                            List.of(
                                    "設定 key 的編碼方式",
                                    "查看 key 對應 value 的內部編碼方式",
                                    "將 key 編碼為 Base64",
                                    "壓縮 key 的儲存空間"
                            ),
                            1,
                            "OBJECT ENCODING 用於查看 key 對應 value 的內部編碼方式（如 int, embstr, raw, hashtable 等）。")
            )
    );

    @Test
    @DisplayName("Quiz 滿分驗證 — 全部答對應通過")
    void quiz_PassesWithFullScore() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // int 用於可解析為 64-bit 整數的值
        answers.put(2, 2);   // O(1)
        answers.put(3, 1);   // field 數量超過 128 或 field value 超過 64 bytes
        answers.put(4, 1);   // Hash 支援 partial update
        answers.put(5, 2);   // Capped Collection
        answers.put(6, 2);   // O(S+N)
        answers.put(7, 2);   // O(N*M)
        answers.put(8, 2);   // skip list + hash table
        answers.put(9, 0);   // NX 只新增，XX 只更新
        answers.put(10, 3);  // Sorted Set
        answers.put(11, 1);  // O(N) 阻塞
        answers.put(12, 2);  // 減少 RTT
        answers.put(13, 1);  // 不需排序用 Set
        answers.put(14, 2);  // listpack 雙向鏈表
        answers.put(15, 1);  // 查看內部編碼

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.correctAnswers()).isEqualTo(15);
    }

    @Test
    @DisplayName("Quiz 80% 正確仍通過 — 12/15 正確")
    void quiz_PassesAt80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct
        answers.put(2, 2);   // correct
        answers.put(3, 1);   // correct
        answers.put(4, 1);   // correct
        answers.put(5, 2);   // correct
        answers.put(6, 2);   // correct
        answers.put(7, 2);   // correct
        answers.put(8, 2);   // correct
        answers.put(9, 0);   // correct
        answers.put(10, 3);  // correct
        answers.put(11, 1);  // correct
        answers.put(12, 2);  // correct
        answers.put(13, 0);  // wrong
        answers.put(14, 0);  // wrong
        answers.put(15, 0);  // wrong

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isTrue();
        assertThat(result.correctAnswers()).isEqualTo(12);
        assertThat(result.score()).isEqualTo(12.0 / 15.0);
    }

    @Test
    @DisplayName("Quiz 低於 80% 不通過 — 11/15 正確")
    void quiz_FailsBelow80Percent() {
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(1, 0);   // correct
        answers.put(2, 2);   // correct
        answers.put(3, 1);   // correct
        answers.put(4, 1);   // correct
        answers.put(5, 2);   // correct
        answers.put(6, 2);   // correct
        answers.put(7, 2);   // correct
        answers.put(8, 2);   // correct
        answers.put(9, 0);   // correct
        answers.put(10, 3);  // correct
        answers.put(11, 1);  // correct
        answers.put(12, 0);  // wrong
        answers.put(13, 0);  // wrong
        answers.put(14, 0);  // wrong
        answers.put(15, 0);  // wrong

        QuizResult result = QuizRunner.run(QUIZ, answers);

        assertThat(result.passed()).isFalse();
        assertThat(result.correctAnswers()).isEqualTo(11);
        assertThat(result.score()).isEqualTo(11.0 / 15.0);
    }
}
