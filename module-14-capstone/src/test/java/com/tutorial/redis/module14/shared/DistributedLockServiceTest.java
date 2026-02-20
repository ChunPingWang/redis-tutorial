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

@DisplayName("DistributedLockService 單元測試")
class DistributedLockServiceTest {

    private DistributedLockPort lockPort;
    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        lockPort = Mockito.mock(DistributedLockPort.class);
        lockService = new DistributedLockService(lockPort);
    }

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
