package com.tutorial.redis.module14.shared;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module14.shared.adapter.outbound.redis.RedisDistributedLockAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisDistributedLockAdapter 整合測試")
class RedisDistributedLockAdapterTest extends AbstractRedisIntegrationTest {

    private RedisDistributedLockAdapter lockAdapter;

    @BeforeEach
    void setUpAdapter() {
        lockAdapter = new RedisDistributedLockAdapter(stringRedisTemplate);
    }

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
