package com.tutorial.redis.module12.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module12.domain.model.VectorSearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisVectorSearchAdapter 整合測試，驗證 Adapter 層的向量儲存與搜尋功能。
 * 測試 Vector Search 的 KNN（K-Nearest Neighbours）搜尋，
 * 使用 COSINE 距離度量找出最相似的向量結果。
 * 屬於 Adapter（外部介面卡）層的測試。
 */
@DisplayName("RedisVectorSearchAdapter 整合測試")
class RedisVectorSearchAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisVectorSearchAdapter adapter;

    // 驗證儲存多組向量後，KNN 搜尋能回傳距離最近的前 K 個結果
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

    // 驗證在無任何向量資料時執行 KNN 搜尋，應回傳空列表
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
