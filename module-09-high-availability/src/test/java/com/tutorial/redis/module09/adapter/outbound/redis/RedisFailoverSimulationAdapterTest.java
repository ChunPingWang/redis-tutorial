package com.tutorial.redis.module09.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 故障轉移模擬適配器整合測試
 * 驗證批次資料寫入與資料完整性驗證功能，模擬故障轉移前後的資料一致性檢查。
 * 屬於 Adapter 層（外部 Redis 介接），測試故障轉移場景下的資料讀寫行為。
 */
@DisplayName("RedisFailoverSimulationAdapter 整合測試")
class RedisFailoverSimulationAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisFailoverSimulationAdapter adapter;

    // 驗證批次寫入 50 筆資料後，資料完整性檢查應確認全部 Key 皆存在
    @Test
    @DisplayName("writeDataBatch_AndVerify_AllKeysExist — 批次寫入 50 筆後驗證完整性，應全部存在")
    void writeDataBatch_AndVerify_AllKeysExist() {
        // Arrange
        adapter.writeDataBatch("failover-test", 50);

        // Act
        int verifiedCount = adapter.verifyDataIntegrity("failover-test", 50);

        // Assert
        assertThat(verifiedCount).isEqualTo(50);
    }

    // 驗證當 Key 不存在時，資料完整性檢查應回傳 0（無資料）
    @Test
    @DisplayName("verifyDataIntegrity_WhenEmpty_ReturnsZero — 無資料時驗證，應回傳 0")
    void verifyDataIntegrity_WhenEmpty_ReturnsZero() {
        // Act
        int result = adapter.verifyDataIntegrity("nonexistent", 10);

        // Assert
        assertThat(result).isEqualTo(0);
    }
}
