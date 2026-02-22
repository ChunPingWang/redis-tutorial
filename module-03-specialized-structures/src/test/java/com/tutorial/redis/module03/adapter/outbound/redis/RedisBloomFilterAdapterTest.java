package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module03.domain.port.outbound.BloomFilterPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis Bloom Filter 配接器整合測試
 * 驗證 BloomFilterPort 透過 Redis Bloom Filter（BF.*）命令的實作正確性
 * 涵蓋新增、查詢、批量操作與假陽性率檢驗，屬於 Adapter 層（外部輸出端）
 */
@DisplayName("RedisBloomFilterAdapter 整合測試")
class RedisBloomFilterAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private BloomFilterPort bloomFilterPort;

    private static final String FILTER_NAME = "test-bloom";

    // 驗證建立 Bloom Filter 後新增元素，mightContain 回傳 true
    @Test
    @DisplayName("createFilter_AndAdd_CanCheckExistence — 建立過濾器並新增後可查詢存在")
    void createFilter_AndAdd_CanCheckExistence() {
        bloomFilterPort.createFilter(FILTER_NAME, 0.01, 1000);

        bloomFilterPort.add(FILTER_NAME, "item1");

        assertThat(bloomFilterPort.mightContain(FILTER_NAME, "item1")).isTrue();
    }

    // 驗證未新增過的元素查詢時回傳 false（不存在時無假陰性）
    @Test
    @DisplayName("mightContain_WhenNotAdded_ReturnsFalse — 未新增的項目回傳 false")
    void mightContain_WhenNotAdded_ReturnsFalse() {
        bloomFilterPort.createFilter(FILTER_NAME, 0.01, 1000);

        assertThat(bloomFilterPort.mightContain(FILTER_NAME, "non-existent-item")).isFalse();
    }

    // 驗證批量新增 10 個元素後，mightContainAll 全部回傳 true
    @Test
    @DisplayName("addAll_WhenMultipleItems_AllContained — 批量新增後全部可查詢")
    void addAll_WhenMultipleItems_AllContained() {
        bloomFilterPort.createFilter(FILTER_NAME, 0.01, 1000);
        List<String> items = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> "item-" + i)
                .toList();

        bloomFilterPort.addAll(FILTER_NAME, items);

        List<Boolean> results = bloomFilterPort.mightContainAll(FILTER_NAME, items);
        assertThat(results).hasSize(10).allMatch(Boolean::booleanValue);
    }

    // 驗證新增 10000 個元素後，對不存在元素的假陽性率低於 2%
    @Test
    @DisplayName("bloomFilter_FalsePositiveRateWithinBounds — 假陽性率在 2% 以內")
    void bloomFilter_FalsePositiveRateWithinBounds() {
        bloomFilterPort.createFilter(FILTER_NAME, 0.01, 10000);

        // Add 10000 items
        for (int i = 0; i < 10000; i++) {
            bloomFilterPort.add(FILTER_NAME, "existing-" + i);
        }

        // Check 10000 non-existing items for false positives
        int falsePositiveCount = 0;
        for (int i = 0; i < 10000; i++) {
            if (bloomFilterPort.mightContain(FILTER_NAME, "non-existing-" + i)) {
                falsePositiveCount++;
            }
        }

        // Allow 2% margin (error rate is 0.01 = 1%, allowing some statistical variance)
        double falsePositiveRate = falsePositiveCount / 10000.0;
        assertThat(falsePositiveRate).isLessThan(0.02);
    }

    // 驗證重複新增同一元素時，第二次 add 回傳 false
    @Test
    @DisplayName("add_WhenAlreadyExists_ReturnsFalse — 重複新增回傳 false")
    void add_WhenAlreadyExists_ReturnsFalse() {
        bloomFilterPort.createFilter(FILTER_NAME, 0.01, 1000);

        boolean firstAdd = bloomFilterPort.add(FILTER_NAME, "duplicate-item");
        boolean secondAdd = bloomFilterPort.add(FILTER_NAME, "duplicate-item");

        assertThat(firstAdd).isTrue();
        assertThat(secondAdd).isFalse();
    }
}
