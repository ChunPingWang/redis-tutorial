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

@DisplayName("ProductSearchService 單元測試")
@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductSearchPort productSearchPort;

    @Mock
    private ProductCachePort productCachePort;

    @InjectMocks
    private ProductSearchService productSearchService;

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
