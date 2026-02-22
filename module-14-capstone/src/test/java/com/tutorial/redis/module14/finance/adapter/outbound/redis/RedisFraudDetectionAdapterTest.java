package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisFraudDetectionAdapter 整合測試類別。
 * 驗證使用 Redis Bloom Filter 進行詐欺交易快速偵測的功能。
 * 展示 BF.ADD/BF.EXISTS 在金融詐欺偵測場景的機率型資料結構應用。
 * 所屬：金融子系統 — adapter 層
 */
@DisplayName("RedisFraudDetectionAdapter 整合測試")
class RedisFraudDetectionAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisFraudDetectionAdapter adapter;

    // 驗證已加入 Bloom Filter 的交易 ID，檢查時應回傳 true（可能存在）
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

    // 驗證未加入 Bloom Filter 的交易 ID，檢查時應回傳 false（一定不存在）
    @Test
    @DisplayName("check_NonExistingItem_ReturnsFalse — 未加入的項目檢查應回傳 false")
    void check_NonExistingItem_ReturnsFalse() {
        // Act — check for an item never added
        boolean result = adapter.mightExist("tx-never-added");

        // Assert — item was never added, Bloom filter should return false
        assertThat(result).isFalse();
    }
}
