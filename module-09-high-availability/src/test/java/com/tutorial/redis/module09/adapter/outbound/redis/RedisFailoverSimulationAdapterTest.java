package com.tutorial.redis.module09.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisFailoverSimulationAdapter 整合測試")
class RedisFailoverSimulationAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisFailoverSimulationAdapter adapter;

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

    @Test
    @DisplayName("verifyDataIntegrity_WhenEmpty_ReturnsZero — 無資料時驗證，應回傳 0")
    void verifyDataIntegrity_WhenEmpty_ReturnsZero() {
        // Act
        int result = adapter.verifyDataIntegrity("nonexistent", 10);

        // Assert
        assertThat(result).isEqualTo(0);
    }
}
