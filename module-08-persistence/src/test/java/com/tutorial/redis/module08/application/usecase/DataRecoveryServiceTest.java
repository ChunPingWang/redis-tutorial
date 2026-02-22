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

/**
 * 測試 DataRecoveryService 的資料恢復模擬邏輯。
 * 驗證 Application 層的用例服務能正確委派寫入與計數操作，並計算資料遺失率。
 * 屬於 Application 層（用例層），使用 Mock 隔離對 Adapter 層的依賴。
 */
@DisplayName("DataRecoveryService 單元測試")
@ExtendWith(MockitoExtension.class)
class DataRecoveryServiceTest {

    @Mock
    private DataRecoveryPort port;

    @InjectMocks
    private DataRecoveryService service;

    // 驗證模擬恢復流程：寫入測試資料後計數，完全恢復時資料遺失率應為 0%
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
