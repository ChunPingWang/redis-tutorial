package com.tutorial.redis.module11.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module11.domain.model.ProductIndex;
import com.tutorial.redis.module11.domain.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisSearchQueryAdapter 整合測試")
class RedisSearchQueryAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisSearchQueryAdapter searchQueryAdapter;

    @Autowired
    private RedisSearchIndexAdapter searchIndexAdapter;

    @Autowired
    private RedisProductDataAdapter productDataAdapter;

    /**
     * Sets up 3 products as Redis Hashes and creates a RediSearch index
     * on the product: prefix with TEXT, TAG, and NUMERIC fields.
     */
    void setupProductsAndIndex() {
        // Save 3 products as hashes
        ProductIndex p1 = new ProductIndex(
                "P001", "Wireless Headphones", "High quality wireless headphones",
                "electronics", 149.99, "Sony");
        ProductIndex p2 = new ProductIndex(
                "P002", "Running Shoes", "Lightweight running shoes",
                "sports", 89.99, "Nike");
        ProductIndex p3 = new ProductIndex(
                "P003", "USB-C Cable", "Fast charging cable",
                "electronics", 19.99, "Anker");
        productDataAdapter.saveProducts(List.of(p1, p2, p3));

        // Create index on product: prefix with weighted schema
        Map<String, String> schema = new LinkedHashMap<>();
        schema.put("name", "TEXT WEIGHT 5.0");
        schema.put("description", "TEXT");
        schema.put("category", "TAG");
        schema.put("price", "NUMERIC SORTABLE");
        schema.put("brand", "TAG");
        searchIndexAdapter.createIndex("idx:products", "product:", schema);
    }

    @Test
    @DisplayName("search_FullText_ReturnsMatches — 全文搜尋 'wireless' 應回傳包含該詞的商品")
    void search_FullText_ReturnsMatches() {
        // Arrange
        setupProductsAndIndex();

        // Act — search for "wireless"
        SearchResult result = searchQueryAdapter.search("idx:products", "wireless");

        // Assert — should find at least 1 product matching "wireless"
        assertThat(result.getTotalResults()).isGreaterThanOrEqualTo(1);
        assertThat(result.getDocuments()).isNotEmpty();
    }

    @Test
    @DisplayName("search_WithLimit_ReturnsLimitedResults — 設定 LIMIT 為 1 時應只回傳 1 筆")
    void search_WithLimit_ReturnsLimitedResults() {
        // Arrange
        setupProductsAndIndex();

        // Act — search all with limit of 1
        SearchResult result = searchQueryAdapter.search("idx:products", "*", 0, 1);

        // Assert — only 1 document returned even though 3 exist
        assertThat(result.getDocuments()).hasSize(1);
    }

    @Test
    @DisplayName("search_NoMatch_ReturnsEmpty — 搜尋不存在的詞應回傳 0 筆結果")
    void search_NoMatch_ReturnsEmpty() {
        // Arrange
        setupProductsAndIndex();

        // Act — search for a term that doesn't exist in any product
        SearchResult result = searchQueryAdapter.search("idx:products", "nonexistent_xyz");

        // Assert — no results
        assertThat(result.getTotalResults()).isEqualTo(0);
        assertThat(result.getDocuments()).isEmpty();
    }
}
