package com.tutorial.redis.module12.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module12.domain.model.VectorSearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisVectorSearchAdapter 整合測試")
class RedisVectorSearchAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisVectorSearchAdapter adapter;

    @Test
    @DisplayName("storeAndSearch_ReturnsNearestVectors — 儲存向量後 KNN 搜尋，應回傳最相似的結果")
    void storeAndSearch_ReturnsNearestVectors() {
        // Arrange — store 3 vectors with different orientations
        // vec:1 and vec:2 are similar (both point roughly in the x-direction)
        // vec:3 is different (points in the z-direction)
        adapter.storeVector("vec:product:P001", "embedding", new float[]{1.0f, 0.0f, 0.0f});
        adapter.storeVector("vec:product:P002", "embedding", new float[]{0.9f, 0.1f, 0.0f});
        adapter.storeVector("vec:product:P003", "embedding", new float[]{0.0f, 0.0f, 1.0f});

        // Act — KNN search for the 2 nearest neighbours to [1.0, 0.0, 0.0]
        List<VectorSearchResult> results = adapter.knnSearch(
                "idx:products-vec", "embedding", new float[]{1.0f, 0.0f, 0.0f}, 2);

        // Assert — should return P001 and P002 as the top-2 most similar
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getDocumentId()).contains("P001");
    }

    @Test
    @DisplayName("knnSearch_WhenEmpty_ReturnsEmpty — 無資料時搜尋，應回傳空列表")
    void knnSearch_WhenEmpty_ReturnsEmpty() {
        // Act — search when no vectors have been stored
        List<VectorSearchResult> results = adapter.knnSearch(
                "idx:vectors", "embedding", new float[]{1.0f, 0.0f, 0.0f}, 5);

        // Assert — should return an empty list
        assertThat(results).isEmpty();
    }
}
