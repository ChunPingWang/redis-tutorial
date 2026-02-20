package com.tutorial.redis.module05.application.usecase;

import com.tutorial.redis.module05.domain.port.outbound.OptimisticLockPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OptimisticBalanceUpdateService 單元測試")
class OptimisticBalanceUpdateServiceTest {

    @Mock
    private OptimisticLockPort optimisticLockPort;

    @InjectMocks
    private OptimisticBalanceUpdateService service;

    @Test
    @DisplayName("updateBalanceWithRetry_SucceedsOnFirstAttempt — 第一次嘗試即成功，只呼叫一次")
    void updateBalanceWithRetry_SucceedsOnFirstAttempt() {
        // Arrange
        when(optimisticLockPort.compareAndSetBalance("ACC-001", 1000.0, 1500.0))
                .thenReturn(true);

        // Act
        boolean result = service.updateBalanceWithRetry("ACC-001", 1000.0, 1500.0, 3);

        // Assert
        assertThat(result).isTrue();
        verify(optimisticLockPort, times(1)).compareAndSetBalance("ACC-001", 1000.0, 1500.0);
    }

    @Test
    @DisplayName("updateBalanceWithRetry_SucceedsOnThirdAttempt — 前兩次失敗第三次成功，共呼叫三次")
    void updateBalanceWithRetry_SucceedsOnThirdAttempt() {
        // Arrange — fail, fail, succeed
        when(optimisticLockPort.compareAndSetBalance("ACC-001", 1000.0, 1500.0))
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);

        // Act
        boolean result = service.updateBalanceWithRetry("ACC-001", 1000.0, 1500.0, 3);

        // Assert
        assertThat(result).isTrue();
        verify(optimisticLockPort, times(3)).compareAndSetBalance("ACC-001", 1000.0, 1500.0);
    }

    @Test
    @DisplayName("updateBalanceWithRetry_FailsAfterMaxRetries — 超過最大重試次數仍失敗，共呼叫 4 次")
    void updateBalanceWithRetry_FailsAfterMaxRetries() {
        // Arrange — always fail
        when(optimisticLockPort.compareAndSetBalance("ACC-001", 1000.0, 1500.0))
                .thenReturn(false);

        // Act — maxRetries=3, so total attempts = 1 (initial) + 3 (retries) = 4
        boolean result = service.updateBalanceWithRetry("ACC-001", 1000.0, 1500.0, 3);

        // Assert
        assertThat(result).isFalse();
        verify(optimisticLockPort, times(4)).compareAndSetBalance("ACC-001", 1000.0, 1500.0);
    }
}
