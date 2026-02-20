package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module03.domain.port.outbound.BloomFilterPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisBloomFilterAdapter 整合測試")
class RedisBloomFilterAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private BloomFilterPort bloomFilterPort;

    private static final String FILTER_NAME = "test-bloom";

    @Test
    @DisplayName("createFilter_AndAdd_CanCheckExistence — 建立過濾器並新增後可查詢存在")
    void createFilter_AndAdd_CanCheckExistence() {
        bloomFilterPort.createFilter(FILTER_NAME, 0.01, 1000);

        bloomFilterPort.add(FILTER_NAME, "item1");

        assertThat(bloomFilterPort.mightContain(FILTER_NAME, "item1")).isTrue();
    }

    @Test
    @DisplayName("mightContain_WhenNotAdded_ReturnsFalse — 未新增的項目回傳 false")
    void mightContain_WhenNotAdded_ReturnsFalse() {
        bloomFilterPort.createFilter(FILTER_NAME, 0.01, 1000);

        assertThat(bloomFilterPort.mightContain(FILTER_NAME, "non-existent-item")).isFalse();
    }

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
