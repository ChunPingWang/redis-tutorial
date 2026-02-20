package com.tutorial.redis.module08.application.usecase;

import com.tutorial.redis.module08.domain.model.RecoveryResult;
import com.tutorial.redis.module08.domain.port.outbound.DataRecoveryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("DataRecoveryService 單元測試")
@ExtendWith(MockitoExtension.class)
class DataRecoveryServiceTest {

    @Mock
    private DataRecoveryPort port;

    @InjectMocks
    private DataRecoveryService service;

    @Test
    @DisplayName("simulateRecovery_WritesAndCountsData — 模擬恢復應寫入資料並計數驗證")
    void simulateRecovery_WritesAndCountsData() {
        // Arrange
        doNothing().when(port).writeTestData(eq("test"), eq(50));
        when(port.countKeys(any())).thenReturn(50);

        // Act
        RecoveryResult result = service.simulateRecovery("test", 50);

        // Assert
        verify(port).writeTestData("test", 50);
        assertThat(result.getKeysRecovered()).isEqualTo(50);
        assertThat(result.getKeysWritten()).isEqualTo(50);
        assertThat(result.getDataLossPercentage()).isEqualTo(0.0);
    }
}
