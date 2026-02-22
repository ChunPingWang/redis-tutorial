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

/**
 * 樂觀鎖餘額更新 Service 單元測試 — 驗證 WATCH 樂觀鎖的重試邏輯。
 * 展示 Redis WATCH 樂觀鎖技術的應用層實作：CAS 失敗時自動重試，超過上限則回傳失敗。
 * 所屬層級：Application 層（use case），使用 Mockito 模擬 Port 進行隔離測試。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OptimisticBalanceUpdateService 單元測試")
class OptimisticBalanceUpdateServiceTest {

    @Mock
    private OptimisticLockPort optimisticLockPort;

    @InjectMocks
    private OptimisticBalanceUpdateService service;

    // 驗證第一次 CAS 即成功時，只呼叫一次 compareAndSetBalance
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

    // 驗證前兩次 CAS 因衝突失敗、第三次成功時，共呼叫三次 compareAndSetBalance
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

    // 驗證持續 CAS 衝突超過最大重試次數後回傳失敗，共嘗試 1+3=4 次
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
