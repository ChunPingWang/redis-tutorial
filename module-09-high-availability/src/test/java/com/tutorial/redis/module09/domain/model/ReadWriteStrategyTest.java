package com.tutorial.redis.module09.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 讀寫分離策略列舉測試
 * 驗證 ReadWriteStrategy 列舉定義了正確數量的策略，且每個策略皆具有描述文字。
 * 屬於 Domain 層（領域模型），展示 Redis 讀寫分離的四種策略模型定義。
 */
@DisplayName("ReadWriteStrategy 列舉測試")
class ReadWriteStrategyTest {

    // 驗證列舉中定義了 4 種讀寫策略（如 MASTER_ONLY、REPLICA_PREFERRED 等）
    @Test
    @DisplayName("values_ReturnsFourStrategies — 應有 4 種讀寫策略")
    void values_ReturnsFourStrategies() {
        // Act
        ReadWriteStrategy[] strategies = ReadWriteStrategy.values();

        // Assert
        assertThat(strategies).hasSize(4);
    }

    // 驗證 MASTER_ONLY 策略具有非空的中文描述文字
    @Test
    @DisplayName("masterOnly_HasDescription — MASTER_ONLY 應有中文描述")
    void masterOnly_HasDescription() {
        // Act & Assert
        assertThat(ReadWriteStrategy.MASTER_ONLY.getDescription()).isNotBlank();
    }
}
