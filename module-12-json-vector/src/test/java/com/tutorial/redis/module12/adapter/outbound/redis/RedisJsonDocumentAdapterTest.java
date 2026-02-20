package com.tutorial.redis.module12.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisJsonDocumentAdapter 整合測試")
class RedisJsonDocumentAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisJsonDocumentAdapter adapter;

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
