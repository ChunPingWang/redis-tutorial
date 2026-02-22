package com.tutorial.redis.module09.domain.service;

import com.tutorial.redis.module09.domain.model.SentinelConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sentinel 配置領域服務測試
 * 驗證 SentinelConfigService 產生的推薦配置參數正確（quorum、masterName 等）。
 * 屬於 Domain 層（領域服務），展示 Redis Sentinel 哨兵的最佳實踐配置建議。
 */
@DisplayName("SentinelConfigService 領域服務測試")
class SentinelConfigServiceTest {

    private final SentinelConfigService service = new SentinelConfigService();

    // 驗證推薦配置的 quorum 值為 2（三節點 Sentinel 的多數決門檻）
    @Test
    @DisplayName("getRecommendedConfig_ReturnsQuorumTwo — 推薦配置的 quorum 應為 2")
    void getRecommendedConfig_ReturnsQuorumTwo() {
        // Act
        SentinelConfig config = service.getRecommendedConfig("mymaster");

        // Assert
        assertThat(config.getQuorum()).isEqualTo(2);
    }

    // 驗證推薦配置中的 masterName 與傳入參數一致
    @Test
    @DisplayName("getRecommendedConfig_SetsCorrectMasterName — 推薦配置應包含正確的 masterName")
    void getRecommendedConfig_SetsCorrectMasterName() {
        // Act
        SentinelConfig config = service.getRecommendedConfig("mymaster");

        // Assert
        assertThat(config.getMasterName()).isEqualTo("mymaster");
    }
}
