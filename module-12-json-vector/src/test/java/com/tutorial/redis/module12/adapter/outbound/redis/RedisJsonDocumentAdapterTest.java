package com.tutorial.redis.module12.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisJsonDocumentAdapter 整合測試，驗證 Adapter 層與 Redis 的 JSON 操作。
 * 測試 RedisJSON 的 JSON.SET、JSON.GET、JSON.NUMINCRBY、JSON.ARRAPPEND 指令，
 * 確保文件的建立、讀取、刪除、數值遞增及陣列追加功能正確運作。
 * 屬於 Adapter（外部介面卡）層的測試。
 */
@DisplayName("RedisJsonDocumentAdapter 整合測試")
class RedisJsonDocumentAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisJsonDocumentAdapter adapter;

    // 驗證 JSON.SET 設定文件後，JSON.GET 能正確讀取回傳
    @Test
    @DisplayName("setAndGetDocument_ReturnsJson — 設定 JSON 文件後讀取，應回傳正確的 JSON")
    void setAndGetDocument_ReturnsJson() {
        // Arrange — set a JSON document at root path
        adapter.setDocument("test:doc1", "$", "{\"name\":\"test\",\"price\":99.99}");

        // Act — retrieve the document
        String result = adapter.getDocument("test:doc1", "$");

        // Assert — should contain the stored values
        assertThat(result).isNotNull();
        assertThat(result).contains("test");
        assertThat(result).contains("99.99");
    }

    // 驗證刪除文件後，再次讀取應回傳 null
    @Test
    @DisplayName("deleteDocument_RemovesData — 刪除 JSON 文件後讀取，應回傳 null")
    void deleteDocument_RemovesData() {
        // Arrange — set a document then delete it
        adapter.setDocument("test:doc2", "$", "{\"name\":\"toDelete\",\"price\":50.00}");
        adapter.deleteDocument("test:doc2");

        // Act — attempt to retrieve the deleted document
        String result = adapter.getDocument("test:doc2", "$");

        // Assert — should be null after deletion
        assertThat(result).isNull();
    }

    // 驗證 JSON.NUMINCRBY 能原子性地遞增 JSON 中的數值欄位
    @Test
    @DisplayName("incrementNumber_UpdatesValue — 數值遞增後，應回傳更新後的值")
    void incrementNumber_UpdatesValue() {
        // Arrange — set a document with a numeric field
        adapter.setDocument("test:doc3", "$", "{\"count\":10}");

        // Act — increment the count by 5
        adapter.incrementNumber("test:doc3", "$.count", 5);

        // Assert — count should now be 15
        String result = adapter.getDocument("test:doc3", "$");
        assertThat(result).isNotNull();
        assertThat(result).contains("15");
    }

    // 驗證 JSON.ARRAPPEND 能在 JSON 陣列末尾追加新元素
    @Test
    @DisplayName("appendToArray_AddsElement — 陣列新增元素後，應包含新元素")
    void appendToArray_AddsElement() {
        // Arrange — set a document with an array field
        adapter.setDocument("test:doc4", "$", "{\"items\":[\"a\"]}");

        // Act — append a new element to the array
        adapter.appendToArray("test:doc4", "$.items", "\"b\"");

        // Assert — the array should contain both "a" and "b"
        String result = adapter.getDocument("test:doc4", "$");
        assertThat(result).isNotNull();
        assertThat(result).contains("a");
        assertThat(result).contains("b");
    }
}
