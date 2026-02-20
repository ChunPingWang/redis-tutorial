package com.tutorial.redis.module11.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module11.domain.model.ProductIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisProductDataAdapter 整合測試")
class RedisProductDataAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisProductDataAdapter productDataAdapter;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("saveProduct_StoresAsHash — 儲存商品後，Redis Hash 應包含所有欄位")
    void saveProduct_StoresAsHash() {
        // Arrange
        ProductIndex product = new ProductIndex(
                "P001", "Wireless Headphones", "High quality wireless headphones",
                "electronics", 149.99, "Sony");

        // Act
        productDataAdapter.saveProduct(product);

        // Assert — verify all fields are stored correctly in the Redis Hash
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries("product:P001");
        assertThat(entries).isNotEmpty();
        assertThat(entries.get("name")).isEqualTo("Wireless Headphones");
        assertThat(entries.get("description")).isEqualTo("High quality wireless headphones");
        assertThat(entries.get("category")).isEqualTo("electronics");
        assertThat(entries.get("price")).isEqualTo("149.99");
        assertThat(entries.get("brand")).isEqualTo("Sony");
    }

    @Test
    @DisplayName("saveProducts_StoresMultiple — 批次儲存 3 筆商品，應全部存在")
    void saveProducts_StoresMultiple() {
        // Arrange
        ProductIndex p1 = new ProductIndex(
                "P001", "Wireless Headphones", "High quality wireless headphones",
                "electronics", 149.99, "Sony");
        ProductIndex p2 = new ProductIndex(
                "P002", "Running Shoes", "Lightweight running shoes",
                "sports", 89.99, "Nike");
        ProductIndex p3 = new ProductIndex(
                "P003", "USB-C Cable", "Fast charging cable",
                "electronics", 19.99, "Anker");

        // Act
        productDataAdapter.saveProducts(List.of(p1, p2, p3));

        // Assert — verify all 3 products exist as hashes
        assertThat(stringRedisTemplate.hasKey("product:P001")).isTrue();
        assertThat(stringRedisTemplate.hasKey("product:P002")).isTrue();
        assertThat(stringRedisTemplate.hasKey("product:P003")).isTrue();

        // Verify individual fields for each product
        Map<Object, Object> entries1 = stringRedisTemplate.opsForHash().entries("product:P001");
        assertThat(entries1.get("name")).isEqualTo("Wireless Headphones");
        assertThat(entries1.get("brand")).isEqualTo("Sony");

        Map<Object, Object> entries2 = stringRedisTemplate.opsForHash().entries("product:P002");
        assertThat(entries2.get("name")).isEqualTo("Running Shoes");
        assertThat(entries2.get("brand")).isEqualTo("Nike");

        Map<Object, Object> entries3 = stringRedisTemplate.opsForHash().entries("product:P003");
        assertThat(entries3.get("name")).isEqualTo("USB-C Cable");
        assertThat(entries3.get("brand")).isEqualTo("Anker");
    }
}
