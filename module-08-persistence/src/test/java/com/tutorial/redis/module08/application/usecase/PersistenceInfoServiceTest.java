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

@DisplayName("PersistenceInfoService 單元測試")
@ExtendWith(MockitoExtension.class)
class PersistenceInfoServiceTest {

    @Mock
    private PersistenceInfoPort port;

    @InjectMocks
    private PersistenceInfoService service;

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
