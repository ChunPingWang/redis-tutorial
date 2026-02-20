package com.tutorial.redis.module11.application.usecase;

import com.tutorial.redis.module11.domain.model.SearchResult;
import com.tutorial.redis.module11.domain.port.outbound.ProductDataPort;
import com.tutorial.redis.module11.domain.port.outbound.SearchIndexPort;
import com.tutorial.redis.module11.domain.port.outbound.SearchQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProductSearchService 單元測試")
@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private SearchIndexPort searchIndexPort;

    @Mock
    private SearchQueryPort searchQueryPort;

    @Mock
    private ProductDataPort productDataPort;

    @InjectMocks
    private ProductSearchService service;

    @Test
    @DisplayName("searchProducts_DelegatesToSearchQueryPort — 搜尋商品應委派給 SearchQueryPort")
    void searchProducts_DelegatesToSearchQueryPort() {
        // Arrange — stub the search query port to return a result
        SearchResult expectedResult = new SearchResult(1L,
                List.of(Map.of("name", "Wireless Headphones", "price", "149.99")));
        when(searchQueryPort.search(eq("idx:products"), eq("wireless"))).thenReturn(expectedResult);

        // Act
        SearchResult result = service.searchProducts("wireless");

        // Assert — verify delegation and correct result
        verify(searchQueryPort).search(eq("idx:products"), eq("wireless"));
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getTotalResults()).isEqualTo(1L);
    }

    @Test
    @DisplayName("searchByCategory_BuildsTagQuery — 依類別搜尋時，查詢應包含 @category:{category}")
    void searchByCategory_BuildsTagQuery() {
        // Arrange — stub with a matching result
        SearchResult expectedResult = new SearchResult(2L,
                List.of(
                        Map.of("name", "Wireless Headphones", "category", "electronics"),
                        Map.of("name", "USB-C Cable", "category", "electronics")
                ));
        when(searchQueryPort.search(eq("idx:products"), contains("@category:{electronics}")))
                .thenReturn(expectedResult);

        // Act
        SearchResult result = service.searchByCategory("electronics");

        // Assert — verify the tag query format is used
        verify(searchQueryPort).search(eq("idx:products"), contains("@category:{electronics}"));
        assertThat(result.getTotalResults()).isEqualTo(2L);
    }
}
