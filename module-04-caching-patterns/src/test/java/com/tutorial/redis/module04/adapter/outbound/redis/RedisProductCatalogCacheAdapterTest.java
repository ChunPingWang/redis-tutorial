package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module04.domain.model.ProductCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisProductCatalogCacheAdapter 整合測試")
class RedisProductCatalogCacheAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisProductCatalogCacheAdapter adapter;

    private static final long TTL_MS = 60_000L; // 1 minute

    private ProductCatalog createProduct(String id) {
        return new ProductCatalog(id, "Product " + id, "Electronics", 999.99, "Test product");
    }

    @Test
    @DisplayName("save_AndFindById_ReturnsProduct — 儲存商品後可查詢回來")
    void save_AndFindById_ReturnsProduct() {
        ProductCatalog product = createProduct("PROD-001");

        adapter.save(product, TTL_MS);
        Optional<ProductCatalog> found = adapter.findById("PROD-001");

        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo("PROD-001");
        assertThat(found.get().getName()).isEqualTo("Product PROD-001");
        assertThat(found.get().getCategory()).isEqualTo("Electronics");
        assertThat(found.get().getPrice()).isEqualTo(999.99);
        assertThat(found.get().getDescription()).isEqualTo("Test product");
    }

    @Test
    @DisplayName("findById_WhenNotExists_ReturnsEmpty — 查詢不存在的商品回傳空")
    void findById_WhenNotExists_ReturnsEmpty() {
        Optional<ProductCatalog> found = adapter.findById("NON-EXISTENT");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("evict_RemovesProduct — 驅逐後商品不再存在快取中")
    void evict_RemovesProduct() {
        ProductCatalog product = createProduct("PROD-002");
        adapter.save(product, TTL_MS);
        assertThat(adapter.findById("PROD-002")).isPresent();

        adapter.evict("PROD-002");

        assertThat(adapter.findById("PROD-002")).isEmpty();
    }

    @Test
    @DisplayName("exists_WhenSaved_ReturnsTrue — 已儲存的商品存在確認為 true")
    void exists_WhenSaved_ReturnsTrue() {
        ProductCatalog product = createProduct("PROD-003");
        adapter.save(product, TTL_MS);

        assertThat(adapter.exists("PROD-003")).isTrue();
    }

    @Test
    @DisplayName("exists_WhenNotSaved_ReturnsFalse — 未儲存的商品存在確認為 false")
    void exists_WhenNotSaved_ReturnsFalse() {
        assertThat(adapter.exists("NON-EXISTENT")).isFalse();
    }
}
