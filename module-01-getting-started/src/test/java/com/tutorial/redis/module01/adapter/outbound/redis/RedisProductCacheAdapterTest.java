package com.tutorial.redis.module01.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module01.domain.model.Product;
import com.tutorial.redis.module01.domain.port.outbound.ProductCachePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisProductCacheAdapter 整合測試")
class RedisProductCacheAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private ProductCachePort productCachePort;

    private Product createTestProduct(String id) {
        return new Product(id, "Test Product", new BigDecimal("29.99"), "Electronics", 100);
    }

    @Test
    @DisplayName("save_WhenValidProduct_StoresInRedis")
    void save_WhenValidProduct_StoresInRedis() {
        Product product = createTestProduct("PROD-001");

        productCachePort.save(product, Duration.ofMinutes(10));

        assertThat(productCachePort.findById("PROD-001")).isPresent();
    }

    @Test
    @DisplayName("findById_WhenProductExists_ReturnsProduct")
    void findById_WhenProductExists_ReturnsProduct() {
        Product product = createTestProduct("PROD-002");
        productCachePort.save(product, Duration.ofMinutes(10));

        Optional<Product> found = productCachePort.findById("PROD-002");

        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo("PROD-002");
        assertThat(found.get().getName()).isEqualTo("Test Product");
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
    }

    @Test
    @DisplayName("findById_WhenProductNotExists_ReturnsEmpty")
    void findById_WhenProductNotExists_ReturnsEmpty() {
        assertThat(productCachePort.findById("NON-EXISTENT")).isEmpty();
    }

    @Test
    @DisplayName("evict_WhenProductExists_RemovesFromRedis")
    void evict_WhenProductExists_RemovesFromRedis() {
        Product product = createTestProduct("PROD-003");
        productCachePort.save(product, Duration.ofMinutes(10));

        productCachePort.evict("PROD-003");

        assertThat(productCachePort.findById("PROD-003")).isEmpty();
    }

    @Test
    @DisplayName("findByIds_WhenMultipleProductsCached_ReturnsAll")
    void findByIds_WhenMultipleProductsCached_ReturnsAll() {
        Product p1 = createTestProduct("PROD-010");
        Product p2 = createTestProduct("PROD-011");
        Product p3 = createTestProduct("PROD-012");
        productCachePort.save(p1, Duration.ofMinutes(10));
        productCachePort.save(p2, Duration.ofMinutes(10));
        productCachePort.save(p3, Duration.ofMinutes(10));

        List<Product> found = productCachePort.findByIds(List.of("PROD-010", "PROD-011", "PROD-012"));

        assertThat(found).hasSize(3);
    }

    @Test
    @DisplayName("findByIds_WhenSomeNotCached_ReturnsOnlyCached")
    void findByIds_WhenSomeNotCached_ReturnsOnlyCached() {
        Product p1 = createTestProduct("PROD-020");
        productCachePort.save(p1, Duration.ofMinutes(10));

        List<Product> found = productCachePort.findByIds(List.of("PROD-020", "PROD-MISSING"));

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getProductId()).isEqualTo("PROD-020");
    }

    @Test
    @DisplayName("save_KeyFollowsNamingConvention")
    void save_KeyFollowsNamingConvention() {
        Product product = createTestProduct("PROD-030");
        productCachePort.save(product, Duration.ofMinutes(10));

        assertThat(stringRedisTemplate.keys("ecommerce:product:PROD-030"))
                .isNotNull()
                .hasSize(1);
    }

    @Test
    @DisplayName("save_WhenTTLExpires_ProductIsEvicted")
    void save_WhenTTLExpires_ProductIsEvicted() throws InterruptedException {
        Product product = createTestProduct("PROD-040");
        productCachePort.save(product, Duration.ofSeconds(1));

        assertThat(productCachePort.findById("PROD-040")).isPresent();

        Thread.sleep(1500);

        assertThat(productCachePort.findById("PROD-040")).isEmpty();
    }
}
