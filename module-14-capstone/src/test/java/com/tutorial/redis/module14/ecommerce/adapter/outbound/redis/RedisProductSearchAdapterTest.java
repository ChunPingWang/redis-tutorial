package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisProductSearchAdapter 整合測試")
class RedisProductSearchAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisProductSearchAdapter adapter;

    @Test
    @DisplayName("indexAndSearch_FindsProduct — 索引產品後搜尋應找到該產品")
    void indexAndSearch_FindsProduct() {
        // Arrange — index a product
        Map<String, String> fields = Map.of(
                "name", "Wireless Mouse",
                "category", "electronics",
                "price", "29.99",
                "description", "Ergonomic wireless mouse with USB receiver"
        );
        adapter.indexProduct("p1", fields);

        // Allow a brief moment for indexing
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        List<String> results = adapter.search("wireless mouse");

        // Assert
        assertThat(results).contains("p1");
    }
}
