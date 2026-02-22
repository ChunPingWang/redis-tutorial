package com.tutorial.redis.module01.application.usecase;

import com.tutorial.redis.module01.domain.model.Product;
import com.tutorial.redis.module01.domain.port.outbound.ProductCachePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 商品快取 Use Case 單元測試
 * 驗證 CacheProductService 正確委派快取操作至 ProductCachePort，
 * 包含預設 TTL 設定、單筆/批次查詢及快取清除邏輯。
 * 層級：Application（Use Case 層）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CacheProductService 單元測試")
class CacheProductServiceTest {

    @Mock
    private ProductCachePort productCachePort;

    @InjectMocks
    private CacheProductService service;

    private Product createProduct(String id) {
        return new Product(id, "Widget", new BigDecimal("9.99"), "Gadgets", 50);
    }

    // 驗證快取商品時使用預設 TTL（30 分鐘）委派至 Port
    @Test
    @DisplayName("cacheProduct_WhenCalled_DelegatesToPortWithDefaultTTL")
    void cacheProduct_WhenCalled_DelegatesToPortWithDefaultTTL() {
        Product product = createProduct("P-001");

        service.cacheProduct(product);

        verify(productCachePort).save(product, Duration.ofMinutes(30));
    }

    // 驗證查詢已快取商品時回傳正確商品資料
    @Test
    @DisplayName("getProduct_WhenCached_ReturnsProduct")
    void getProduct_WhenCached_ReturnsProduct() {
        Product product = createProduct("P-002");
        when(productCachePort.findById("P-002")).thenReturn(Optional.of(product));

        Optional<Product> result = service.getProduct("P-002");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Widget");
    }

    // 驗證查詢未快取商品時回傳空值
    @Test
    @DisplayName("getProduct_WhenNotCached_ReturnsEmpty")
    void getProduct_WhenNotCached_ReturnsEmpty() {
        when(productCachePort.findById("P-999")).thenReturn(Optional.empty());

        assertThat(service.getProduct("P-999")).isEmpty();
    }

    // 驗證批次查詢多筆商品時正確委派至 Port
    @Test
    @DisplayName("getProducts_WhenMultipleIds_DelegatesToPort")
    void getProducts_WhenMultipleIds_DelegatesToPort() {
        List<String> ids = List.of("P-010", "P-011");
        List<Product> products = List.of(createProduct("P-010"), createProduct("P-011"));
        when(productCachePort.findByIds(ids)).thenReturn(products);

        List<Product> result = service.getProducts(ids);

        assertThat(result).hasSize(2);
        verify(productCachePort).findByIds(ids);
    }

    // 驗證清除商品快取時正確委派至 Port
    @Test
    @DisplayName("evictProduct_WhenCalled_DelegatesToPort")
    void evictProduct_WhenCalled_DelegatesToPort() {
        service.evictProduct("P-003");

        verify(productCachePort).evict("P-003");
    }
}
