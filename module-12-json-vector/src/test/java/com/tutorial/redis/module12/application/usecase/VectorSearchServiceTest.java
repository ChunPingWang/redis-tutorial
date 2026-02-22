package com.tutorial.redis.module12.application.usecase;

import com.tutorial.redis.module12.domain.model.VectorSearchResult;
import com.tutorial.redis.module12.domain.port.outbound.VectorSearchPort;
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

/**
 * VectorSearchService 單元測試，驗證 Application 層的向量搜尋業務邏輯。
 * 使用 Mock 確認 Service 正確委派向量儲存與 KNN 搜尋操作給 VectorSearchPort，
 * 並驗證搜尋結果的正確傳遞。
 * 屬於 Application（應用服務）層的測試。
 */
@DisplayName("VectorSearchService 單元測試")
@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private VectorSearchPort vectorSearchPort;

    @InjectMocks
    private VectorSearchService service;

    // 驗證儲存商品向量時，Service 正確委派給 VectorSearchPort
    @Test
    @DisplayName("storeProductVector_DelegatesToPort — 儲存商品向量應委派給 VectorSearchPort")
    void storeProductVector_DelegatesToPort() {
        // Arrange
        float[] embedding = {1.0f, 2.0f, 3.0f};

        // Act — store a product vector
        service.storeProductVector("P001", embedding);

        // Assert — should delegate to the port with key containing P001
        verify(vectorSearchPort).storeVector(contains("P001"), eq("embedding"), eq(embedding));
    }

    // 驗證搜尋相似商品時，Service 委派 KNN 搜尋並正確回傳結果
    @Test
    @DisplayName("searchSimilarProducts_DelegatesToPort — 搜尋相似商品應委派給 VectorSearchPort.knnSearch")
    void searchSimilarProducts_DelegatesToPort() {
        // Arrange — stub the port to return results
        float[] queryVector = {1.0f, 0.0f, 0.0f};
        List<VectorSearchResult> expectedResults = List.of(
                new VectorSearchResult("product:P001", 0.95, Map.of("name", "Product A")),
                new VectorSearchResult("product:P002", 0.85, Map.of("name", "Product B"))
        );
        when(vectorSearchPort.knnSearch(eq("idx:products-vec"), eq("embedding"), eq(queryVector), eq(3)))
                .thenReturn(expectedResults);

        // Act — search for similar products
        List<VectorSearchResult> results = service.searchSimilarProducts(queryVector, 3);

        // Assert — should delegate to the port and return the results
        verify(vectorSearchPort).knnSearch(eq("idx:products-vec"), eq("embedding"), eq(queryVector), eq(3));
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getDocumentId()).isEqualTo("product:P001");
        assertThat(results.get(0).getScore()).isEqualTo(0.95);
    }
}
