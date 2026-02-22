package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 快取擊穿保護適配器整合測試。
 * 驗證使用分散式鎖（SETNX）實現的 Cache Stampede Protection 模式。
 * 確保同一時間只有一個請求能取得鎖來重建快取，避免大量並發請求湧入資料庫。
 * 屬於 Adapter 層（外部基礎設施適配器）。
 */
@DisplayName("RedisCacheStampedeProtectionAdapter 整合測試")
class RedisCacheStampedeProtectionAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisCacheStampedeProtectionAdapter adapter;

    // 驗證在無鎖狀態下，第一次取得分散式鎖應成功回傳 true
    @Test
    @DisplayName("tryLock_WhenNotLocked_ReturnsTrue — 未上鎖時取得鎖成功")
    void tryLock_WhenNotLocked_ReturnsTrue() {
        boolean acquired = adapter.tryLock("product:123", 5000);

        assertThat(acquired).isTrue();
    }

    // 驗證鎖已被持有時，第二次嘗試取鎖應失敗回傳 false
    @Test
    @DisplayName("tryLock_WhenAlreadyLocked_ReturnsFalse — 已上鎖時取得鎖失敗")
    void tryLock_WhenAlreadyLocked_ReturnsFalse() {
        adapter.tryLock("product:456", 5000);

        boolean secondAttempt = adapter.tryLock("product:456", 5000);

        assertThat(secondAttempt).isFalse();
    }

    // 驗證手動解鎖後，其他請求可以重新取得該鎖
    @Test
    @DisplayName("unlock_ReleasesLock — 解鎖後可重新取得鎖")
    void unlock_ReleasesLock() {
        adapter.tryLock("product:789", 5000);

        adapter.unlock("product:789");

        boolean reacquired = adapter.tryLock("product:789", 5000);
        assertThat(reacquired).isTrue();
    }

    // 驗證鎖的 TTL 過期後會自動釋放，避免死鎖問題
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
