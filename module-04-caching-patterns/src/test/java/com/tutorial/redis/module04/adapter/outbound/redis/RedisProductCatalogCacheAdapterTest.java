package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module04.domain.model.ProductCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 商品目錄快取適配器整合測試。
 * 驗證 Cache-Aside 模式下商品資料的快取存取、驅逐與存在性檢查。
 * 搭配 TTL 策略確保快取資料的有效性。
 * 屬於 Adapter 層（外部基礎設施適配器）。
 */
@DisplayName("RedisProductCatalogCacheAdapter 整合測試")
class RedisProductCatalogCacheAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisProductCatalogCacheAdapter adapter;

    private static final long TTL_MS = 60_000L; // 1 minute

    private ProductCatalog createProduct(String id) {
        return new ProductCatalog(id, "Product " + id, "Electronics", 999.99, "Test product");
    }

    // 驗證商品儲存至 Redis 快取後，可透過商品 ID 正確查詢所有欄位
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

    // 驗證查詢不存在的商品 ID 時回傳 Optional.empty()
    @Test
    @DisplayName("findById_WhenNotExists_ReturnsEmpty — 查詢不存在的商品回傳空")
    void findById_WhenNotExists_ReturnsEmpty() {
        Optional<ProductCatalog> found = adapter.findById("NON-EXISTENT");

        assertThat(found).isEmpty();
    }

    // 驗證快取驅逐操作能正確移除指定商品的快取資料
    @Test
    @DisplayName("evict_RemovesProduct — 驅逐後商品不再存在快取中")
    void evict_RemovesProduct() {
        ProductCatalog product = createProduct("PROD-002");
        adapter.save(product, TTL_MS);
        assertThat(adapter.findById("PROD-002")).isPresent();

        adapter.evict("PROD-002");

        assertThat(adapter.findById("PROD-002")).isEmpty();
    }

    // 驗證已儲存的商品透過 exists 檢查回傳 true
    @Test
    @DisplayName("exists_WhenSaved_ReturnsTrue — 已儲存的商品存在確認為 true")
    void exists_WhenSaved_ReturnsTrue() {
        ProductCatalog product = createProduct("PROD-003");
        adapter.save(product, TTL_MS);

        assertThat(adapter.exists("PROD-003")).isTrue();
    }

    // 驗證未儲存的商品透過 exists 檢查回傳 false
    @Test
    @DisplayName("exists_WhenNotSaved_ReturnsFalse — 未儲存的商品存在確認為 false")
    void exists_WhenNotSaved_ReturnsFalse() {
        assertThat(adapter.exists("NON-EXISTENT")).isFalse();
    }
}
