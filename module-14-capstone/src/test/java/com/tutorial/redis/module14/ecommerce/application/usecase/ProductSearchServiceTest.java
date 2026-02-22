package com.tutorial.redis.module14.ecommerce.application.usecase;

import com.tutorial.redis.module14.ecommerce.domain.model.Product;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.ProductCachePort;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.ProductSearchPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProductSearchService 應用層單元測試類別。
 * 驗證產品索引（快取+搜尋+自動完成）與產品搜尋的業務邏輯。
 * 展示 RediSearch 全文搜尋與 Redis Hash 快取的協同應用。
 * 所屬：電商子系統 — application 層
 */
@DisplayName("ProductSearchService 單元測試")
@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductSearchPort productSearchPort;

    @Mock
    private ProductCachePort productCachePort;

    @InjectMocks
    private ProductSearchService productSearchService;

    // 驗證索引產品時同時進行快取、搜尋索引建立與自動完成建議新增
    @Test
    @DisplayName("indexProduct_CachesAndIndexes — 索引產品應同時快取與建立搜尋索引")
    void indexProduct_CachesAndIndexes() {
        // Arrange
        Product product = new Product("p1", "Widget", "electronics",
                29.99, "A great widget", 100);

        // Act
        productSearchService.indexProduct(product);

        // Assert — verify caching, indexing, and autocomplete suggestion
        verify(productCachePort).cacheProduct(eq("p1"), anyString());
        verify(productSearchPort).indexProduct(eq("p1"), anyMap());
        verify(productSearchPort).addSuggestion("Widget", 1.0);
    }

    // 驗證搜尋產品時正確委派給 ProductSearchPort 並回傳搜尋結果
    @Test
    @DisplayName("searchProducts_DelegatesToSearchPort — 搜尋產品應委派給 ProductSearchPort")
    void searchProducts_DelegatesToSearchPort() {
        // Arrange
        when(productSearchPort.search("widget")).thenReturn(List.of("p1", "p2"));

        // Act
        List<String> results = productSearchService.searchProducts("widget");

        // Assert
        assertThat(results).containsExactly("p1", "p2");
        verify(productSearchPort).search("widget");
    }
}
