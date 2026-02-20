package com.tutorial.redis.module05.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisPipelineAdapter 整合測試")
class RedisPipelineAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisPipelineAdapter adapter;

    @Test
    @DisplayName("batchSetAndGet_ReturnsAllPrices — 批次設定 5 筆價格後全部取回驗證")
    void batchSetAndGet_ReturnsAllPrices() {
        // Arrange
        Map<String, Double> prices = Map.of(
                "PROD-001", 99.99,
                "PROD-002", 149.50,
                "PROD-003", 29.00,
                "PROD-004", 599.99,
                "PROD-005", 12.75
        );

        // Act
        adapter.batchSetPrices(prices);
        Map<String, Double> result = adapter.batchGetPrices(
                List.of("PROD-001", "PROD-002", "PROD-003", "PROD-004", "PROD-005")
        );

        // Assert
        assertThat(result).hasSize(5);
        assertThat(result.get("PROD-001")).isEqualTo(99.99);
        assertThat(result.get("PROD-002")).isEqualTo(149.50);
        assertThat(result.get("PROD-003")).isEqualTo(29.00);
        assertThat(result.get("PROD-004")).isEqualTo(599.99);
        assertThat(result.get("PROD-005")).isEqualTo(12.75);

        // Verify keys exist in Redis
        assertThat(stringRedisTemplate.opsForValue().get("price:PROD-001")).isEqualTo("99.99");
        assertThat(stringRedisTemplate.opsForValue().get("price:PROD-005")).isEqualTo("12.75");
    }

    @Test
    @DisplayName("batchGetPrices_WhenSomeNotExist_ReturnsNullForMissing — 部分 key 不存在時回傳 null")
    void batchGetPrices_WhenSomeNotExist_ReturnsNullForMissing() {
        // Arrange — only set 2 out of 3 products
        Map<String, Double> prices = Map.of(
                "EXIST-001", 50.00,
                "EXIST-002", 75.00
        );
        adapter.batchSetPrices(prices);

        // Act — query 3 products (1 missing)
        Map<String, Double> result = adapter.batchGetPrices(
                List.of("EXIST-001", "EXIST-002", "MISSING-001")
        );

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get("EXIST-001")).isEqualTo(50.00);
        assertThat(result.get("EXIST-002")).isEqualTo(75.00);
        assertThat(result).containsKey("MISSING-001");
        assertThat(result.get("MISSING-001")).isNull();
    }

    @Test
    @DisplayName("batchSetPrices_WhenLargeBatch_CompletesSuccessfully — 100 筆大批量操作正常完成")
    void batchSetPrices_WhenLargeBatch_CompletesSuccessfully() {
        // Arrange — 100 products
        Map<String, Double> prices = new HashMap<>();
        List<String> productIds = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> "BULK-" + String.format("%03d", i))
                .toList();

        for (int i = 0; i < productIds.size(); i++) {
            prices.put(productIds.get(i), (i + 1) * 10.0);
        }

        // Act
        adapter.batchSetPrices(prices);
        Map<String, Double> result = adapter.batchGetPrices(productIds);

        // Assert
        assertThat(result).hasSize(100);
        for (int i = 0; i < productIds.size(); i++) {
            String productId = productIds.get(i);
            assertThat(result.get(productId))
                    .as("Price for %s", productId)
                    .isEqualTo((i + 1) * 10.0);
        }
    }
}
