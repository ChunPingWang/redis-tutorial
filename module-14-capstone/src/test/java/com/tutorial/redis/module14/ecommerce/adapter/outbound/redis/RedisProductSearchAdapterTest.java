package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisProductSearchAdapter 整合測試類別。
 * 驗證使用 RediSearch 建立產品索引並進行全文搜尋的功能。
 * 展示 FT.CREATE/FT.SEARCH 在電商產品搜尋場景的應用。
 * 所屬：電商子系統 — adapter 層
 */
@DisplayName("RedisProductSearchAdapter 整合測試")
class RedisProductSearchAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisProductSearchAdapter adapter;

    // 驗證將產品索引至 RediSearch 後，以關鍵字搜尋能找到對應產品
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
