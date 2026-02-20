package com.tutorial.redis.module09.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReadWriteStrategy 列舉測試")
class ReadWriteStrategyTest {

    @Test
    @DisplayName("values_ReturnsFourStrategies — 應有 4 種讀寫策略")
    void values_ReturnsFourStrategies() {
        // Act
        ReadWriteStrategy[] strategies = ReadWriteStrategy.values();

        // Assert
        assertThat(strategies).hasSize(4);
    }

    @Test
    @DisplayName("masterOnly_HasDescription — MASTER_ONLY 應有中文描述")
    void masterOnly_HasDescription() {
        // Act & Assert
        assertThat(ReadWriteStrategy.MASTER_ONLY.getDescription()).isNotBlank();
    }
}
