package com.tutorial.redis.module14.shared;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module14.shared.adapter.outbound.redis.RedisDistributedLockAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisDistributedLockAdapter 整合測試類別。
 * 驗證使用 Redis SET NX EX 實作分散式鎖的取得、釋放與互斥功能。
 * 展示 Redis 原子性操作與 Lua 腳本在分散式鎖場景的應用。
 * 所屬：共用分散式模式 — shared 層
 */
@DisplayName("RedisDistributedLockAdapter 整合測試")
class RedisDistributedLockAdapterTest extends AbstractRedisIntegrationTest {

    private RedisDistributedLockAdapter lockAdapter;

    @BeforeEach
    void setUpAdapter() {
        lockAdapter = new RedisDistributedLockAdapter(stringRedisTemplate);
    }

    // 驗證正常流程：取得鎖成功後，由同一擁有者釋放鎖也應成功
    @Test
    @DisplayName("tryLockAndUnlock_WorksCorrectly — 取得鎖後釋放應成功")
    void tryLockAndUnlock_WorksCorrectly() {
        // Act - acquire
        boolean acquired = lockAdapter.tryLock("test-lock", "owner-1", 30);

        // Assert - acquired
        assertThat(acquired).isTrue();

        // Act - release
        boolean released = lockAdapter.unlock("test-lock", "owner-1");

        // Assert - released
        assertThat(released).isTrue();
    }

    // 驗證互斥性：當鎖已被其他擁有者持有時，嘗試取得應回傳 false
    @Test
    @DisplayName("tryLock_WhenAlreadyLocked_ReturnsFalse — 已被鎖定時應回傳 false")
    void tryLock_WhenAlreadyLocked_ReturnsFalse() {
        // Arrange - first lock
        lockAdapter.tryLock("test-lock", "owner-1", 30);

        // Act - try to lock with different owner
        boolean acquired = lockAdapter.tryLock("test-lock", "owner-2", 30);

        // Assert
        assertThat(acquired).isFalse();
    }

    // 驗證安全性：非鎖擁有者嘗試釋放鎖應被拒絕（回傳 false）
    @Test
    @DisplayName("unlock_WithWrongOwner_ReturnsFalse — 非擁有者釋放鎖應回傳 false")
    void unlock_WithWrongOwner_ReturnsFalse() {
        // Arrange
        lockAdapter.tryLock("test-lock", "owner-1", 30);

        // Act - try to unlock with wrong owner
        boolean released = lockAdapter.unlock("test-lock", "owner-2");

        // Assert
        assertThat(released).isFalse();
    }
}
