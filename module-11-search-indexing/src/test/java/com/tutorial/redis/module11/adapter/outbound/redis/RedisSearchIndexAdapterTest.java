package com.tutorial.redis.module11.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisSearchIndexAdapter 整合測試")
class RedisSearchIndexAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisSearchIndexAdapter searchIndexAdapter;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("createIndex_AndCheckExists_ReturnsTrue — 建立索引後檢查存在，應回傳 true")
    void createIndex_AndCheckExists_ReturnsTrue() {
        // Arrange — define a simple schema with TEXT and NUMERIC fields
        Map<String, String> schema = new LinkedHashMap<>();
        schema.put("name", "TEXT");
        schema.put("price", "NUMERIC");

        // Act — create index on product: prefix
        searchIndexAdapter.createIndex("idx:test-products", "product:", schema);

        // Assert — indexExists should return true
        boolean exists = searchIndexAdapter.indexExists("idx:test-products");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("indexExists_WhenNotCreated_ReturnsFalse — 未建立索引時檢查，應回傳 false")
    void indexExists_WhenNotCreated_ReturnsFalse() {
        // Act — check for a non-existent index
        boolean exists = searchIndexAdapter.indexExists("idx:nonexistent-index");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("dropIndex_RemovesIndex — 建立索引後刪除，應不再存在")
    void dropIndex_RemovesIndex() {
        // Arrange — create an index first
        Map<String, String> schema = new LinkedHashMap<>();
        schema.put("name", "TEXT");
        schema.put("price", "NUMERIC");
        searchIndexAdapter.createIndex("idx:drop-test", "product:", schema);

        // Verify index was created
        assertThat(searchIndexAdapter.indexExists("idx:drop-test")).isTrue();

        // Act — drop the index
        searchIndexAdapter.dropIndex("idx:drop-test");

        // Assert — index should no longer exist
        assertThat(searchIndexAdapter.indexExists("idx:drop-test")).isFalse();
    }
}
