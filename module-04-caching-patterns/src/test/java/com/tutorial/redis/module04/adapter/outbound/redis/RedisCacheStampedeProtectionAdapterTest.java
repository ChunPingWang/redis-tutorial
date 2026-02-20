package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisCacheStampedeProtectionAdapter 整合測試")
class RedisCacheStampedeProtectionAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisCacheStampedeProtectionAdapter adapter;

    @Test
    @DisplayName("tryLock_WhenNotLocked_ReturnsTrue — 未上鎖時取得鎖成功")
    void tryLock_WhenNotLocked_ReturnsTrue() {
        boolean acquired = adapter.tryLock("product:123", 5000);

        assertThat(acquired).isTrue();
    }

    @Test
    @DisplayName("tryLock_WhenAlreadyLocked_ReturnsFalse — 已上鎖時取得鎖失敗")
    void tryLock_WhenAlreadyLocked_ReturnsFalse() {
        adapter.tryLock("product:456", 5000);

        boolean secondAttempt = adapter.tryLock("product:456", 5000);

        assertThat(secondAttempt).isFalse();
    }

    @Test
    @DisplayName("unlock_ReleasesLock — 解鎖後可重新取得鎖")
    void unlock_ReleasesLock() {
        adapter.tryLock("product:789", 5000);

        adapter.unlock("product:789");

        boolean reacquired = adapter.tryLock("product:789", 5000);
        assertThat(reacquired).isTrue();
    }

    @Test
    @DisplayName("tryLock_ExpiresAfterTtl — 鎖在 TTL 過期後自動釋放")
    void tryLock_ExpiresAfterTtl() throws InterruptedException {
        adapter.tryLock("product:ttl", 1000); // 1 second TTL

        // Lock should be held
        assertThat(adapter.tryLock("product:ttl", 1000)).isFalse();

        // Wait for TTL to expire
        Thread.sleep(1500);

        // Lock should be auto-released
        boolean reacquired = adapter.tryLock("product:ttl", 5000);
        assertThat(reacquired).isTrue();
    }
}
