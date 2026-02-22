package com.tutorial.redis.module08.application.usecase;

import com.tutorial.redis.module08.domain.model.PersistenceStatus;
import com.tutorial.redis.module08.domain.port.outbound.PersistenceInfoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 測試 PersistenceInfoService 的持久化資訊查詢與快照觸發邏輯。
 * 驗證 Application 層能正確委派取得 RDB/AOF 狀態及觸發 BGSAVE 操作。
 * 屬於 Application 層（用例層），使用 Mock 隔離對 Port 介面的依賴。
 */
@DisplayName("PersistenceInfoService 單元測試")
@ExtendWith(MockitoExtension.class)
class PersistenceInfoServiceTest {

    @Mock
    private PersistenceInfoPort port;

    @InjectMocks
    private PersistenceInfoService service;

    // 驗證取得持久化狀態時，服務層正確委派給 PersistenceInfoPort 並回傳結果
    @Test
    @DisplayName("getPersistenceStatus_DelegatesToPort — 取得持久化狀態應委派給 PersistenceInfoPort")
    void getPersistenceStatus_DelegatesToPort() {
        // Arrange
        PersistenceStatus expected = new PersistenceStatus(true, false, 1700000000L, 0L, false, "ok");
        when(port.getPersistenceStatus()).thenReturn(expected);

        // Act
        PersistenceStatus result = service.getPersistenceStatus();

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(port, times(1)).getPersistenceStatus();
    }

    // 驗證觸發 RDB 快照時，服務層正確呼叫 Port 的 triggerBgsave 方法
    @Test
    @DisplayName("triggerRdbSnapshot_DelegatesToPort — 觸發 BGSAVE 應委派給 PersistenceInfoPort.triggerBgsave")
    void triggerRdbSnapshot_DelegatesToPort() {
        // Arrange
        doNothing().when(port).triggerBgsave();

        // Act
        service.triggerRdbSnapshot();

        // Assert
        verify(port, times(1)).triggerBgsave();
    }
}
