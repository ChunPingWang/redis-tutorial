package com.tutorial.redis.module08.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("RecoveryResult 領域模型測試")
class RecoveryResultTest {

    @Test
    @DisplayName("dataLossPercentage_FullRecovery_IsZero — 完全恢復時資料遺失百分比應為 0")
    void dataLossPercentage_FullRecovery_IsZero() {
        // Arrange & Act
        RecoveryResult result = new RecoveryResult("rdb", 100, 100, 50);

        // Assert
        assertThat(result.getDataLossPercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("dataLossPercentage_PartialRecovery_CalculatedCorrectly — 部分恢復時應正確計算遺失百分比")
    void dataLossPercentage_PartialRecovery_CalculatedCorrectly() {
        // Arrange & Act
        RecoveryResult result = new RecoveryResult("aof", 100, 90, 50);

        // Assert
        assertThat(result.getDataLossPercentage()).isCloseTo(10.0, within(0.001));
    }
}
