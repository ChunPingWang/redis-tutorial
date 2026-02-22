package com.tutorial.redis.module08.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 測試 RecoveryResult 領域模型的資料遺失百分比計算邏輯。
 * 驗證 RDB/AOF 持久化恢復後，根據寫入數與恢復數正確計算資料遺失率。
 * 屬於 Domain 層（領域模型），為純粹的業務邏輯單元測試。
 */
@DisplayName("RecoveryResult 領域模型測試")
class RecoveryResultTest {

    // 驗證寫入 100 筆且恢復 100 筆時，資料遺失百分比為 0%
    @Test
    @DisplayName("dataLossPercentage_FullRecovery_IsZero — 完全恢復時資料遺失百分比應為 0")
    void dataLossPercentage_FullRecovery_IsZero() {
        // Arrange & Act
        RecoveryResult result = new RecoveryResult("rdb", 100, 100, 50);

        // Assert
        assertThat(result.getDataLossPercentage()).isEqualTo(0.0);
    }

    // 驗證寫入 100 筆但僅恢復 90 筆時，資料遺失百分比應為 10%
    @Test
    @DisplayName("dataLossPercentage_PartialRecovery_CalculatedCorrectly — 部分恢復時應正確計算遺失百分比")
    void dataLossPercentage_PartialRecovery_CalculatedCorrectly() {
        // Arrange & Act
        RecoveryResult result = new RecoveryResult("aof", 100, 90, 50);

        // Assert
        assertThat(result.getDataLossPercentage()).isCloseTo(10.0, within(0.001));
    }
}
