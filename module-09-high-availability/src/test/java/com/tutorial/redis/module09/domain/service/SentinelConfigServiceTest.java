package com.tutorial.redis.module09.domain.service;

import com.tutorial.redis.module09.domain.model.SentinelConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SentinelConfigService 領域服務測試")
class SentinelConfigServiceTest {

    private final SentinelConfigService service = new SentinelConfigService();

    @Test
    @DisplayName("getRecommendedConfig_ReturnsQuorumTwo — 推薦配置的 quorum 應為 2")
    void getRecommendedConfig_ReturnsQuorumTwo() {
        // Act
        SentinelConfig config = service.getRecommendedConfig("mymaster");

        // Assert
        assertThat(config.getQuorum()).isEqualTo(2);
    }

    @Test
    @DisplayName("getRecommendedConfig_SetsCorrectMasterName — 推薦配置應包含正確的 masterName")
    void getRecommendedConfig_SetsCorrectMasterName() {
        // Act
        SentinelConfig config = service.getRecommendedConfig("mymaster");

        // Assert
        assertThat(config.getMasterName()).isEqualTo("mymaster");
    }
}
