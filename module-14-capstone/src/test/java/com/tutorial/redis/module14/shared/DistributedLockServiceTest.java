package com.tutorial.redis.module14.shared;

import com.tutorial.redis.module14.shared.application.usecase.DistributedLockService;
import com.tutorial.redis.module14.shared.domain.port.outbound.DistributedLockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DistributedLockService 應用層單元測試類別。
 * 驗證分散式鎖服務的取得鎖與釋放鎖邏輯，包含 Key 前綴組裝。
 * 展示使用 Redis SET NX EX 實現分散式互斥鎖的應用層封裝。
 * 所屬：共用分散式模式 — shared 層
 */
@DisplayName("DistributedLockService 單元測試")
class DistributedLockServiceTest {

    private DistributedLockPort lockPort;
    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        lockPort = Mockito.mock(DistributedLockPort.class);
        lockService = new DistributedLockService(lockPort);
    }

    // 驗證取得鎖時，服務層自動加上 lock: 前綴並委派給 DistributedLockPort
    @Test
    @DisplayName("acquireLock_DelegatesToPort — 取得鎖應委派給 Port 並加上 lock: 前綴")
    void acquireLock_DelegatesToPort() {
        // Arrange
        when(lockPort.tryLock("lock:resource-1", "owner-1", 30)).thenReturn(true);

        // Act
        boolean result = lockService.acquireLock("resource-1", "owner-1", 30);

        // Assert
        assertThat(result).isTrue();
        verify(lockPort).tryLock("lock:resource-1", "owner-1", 30);
    }

    // 驗證釋放鎖時，服務層自動加上 lock: 前綴並委派給 DistributedLockPort
    @Test
    @DisplayName("releaseLock_DelegatesToPort — 釋放鎖應委派給 Port 並加上 lock: 前綴")
    void releaseLock_DelegatesToPort() {
        // Arrange
        when(lockPort.unlock("lock:resource-1", "owner-1")).thenReturn(true);

        // Act
        boolean result = lockService.releaseLock("resource-1", "owner-1");

        // Assert
        assertThat(result).isTrue();
        verify(lockPort).unlock("lock:resource-1", "owner-1");
    }
}
