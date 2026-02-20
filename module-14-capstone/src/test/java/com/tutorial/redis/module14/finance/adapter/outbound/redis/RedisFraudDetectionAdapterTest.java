package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisFraudDetectionAdapter 整合測試")
class RedisFraudDetectionAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisFraudDetectionAdapter adapter;

    @Test
    @DisplayName("addAndCheck_ExistingItem_ReturnsTrue — 加入項目後檢查應回傳 true")
    void addAndCheck_ExistingItem_ReturnsTrue() {
        // Arrange — add a transaction to the Bloom filter
        adapter.addToBloomFilter("tx-fraud-001");

        // Act
        boolean result = adapter.mightExist("tx-fraud-001");

        // Assert — item was added, so it must exist
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("check_NonExistingItem_ReturnsFalse — 未加入的項目檢查應回傳 false")
    void check_NonExistingItem_ReturnsFalse() {
        // Act — check for an item never added
        boolean result = adapter.mightExist("tx-never-added");

        // Assert — item was never added, Bloom filter should return false
        assertThat(result).isFalse();
    }
}
