package com.tutorial.redis.module05.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 樂觀鎖 Adapter 整合測試 — 驗證透過 WATCH + MULTI/EXEC 實現的 CAS（Compare-And-Set）操作。
 * 展示 Redis WATCH 樂觀鎖技術：監視 key 後執行交易，若 key 被其他客戶端修改則交易失敗。
 * 所屬層級：Adapter 層（outbound），負責以 WATCH 樂觀鎖機制更新 Redis 中的帳戶餘額。
 */
@DisplayName("RedisOptimisticLockAdapter 整合測試")
class RedisOptimisticLockAdapterTest extends AbstractRedisIntegrationTest {

    private static final String ACCOUNT_KEY = "account:balance:ACC-001";

    @Autowired
    private RedisOptimisticLockAdapter adapter;

    @BeforeEach
    void setUpAccount() {
        stringRedisTemplate.opsForValue().set(ACCOUNT_KEY, "1000.0");
    }

    // 驗證當前餘額與預期值匹配時，WATCH + MULTI/EXEC 的 CAS 操作成功更新餘額
    @Test
    @DisplayName("compareAndSet_WhenExpectedMatches_ReturnsTrue — 預期值匹配時 CAS 成功")
    void compareAndSet_WhenExpectedMatches_ReturnsTrue() {
        // Act
        boolean result = adapter.compareAndSetBalance("ACC-001", 1000.0, 1500.0);

        // Assert
        assertThat(result).isTrue();
        String newValue = stringRedisTemplate.opsForValue().get(ACCOUNT_KEY);
        assertThat(newValue).isEqualTo("1500.0");
    }

    // 驗證當前餘額與預期值不匹配時，CAS 操作失敗且原始值維持不變
    @Test
    @DisplayName("compareAndSet_WhenExpectedMismatch_ReturnsFalse — 預期值不匹配時 CAS 失敗且值不變")
    void compareAndSet_WhenExpectedMismatch_ReturnsFalse() {
        // Act — expected is 2000.0 but actual is 1000.0
        boolean result = adapter.compareAndSetBalance("ACC-001", 2000.0, 1500.0);

        // Assert
        assertThat(result).isFalse();
        String currentValue = stringRedisTemplate.opsForValue().get(ACCOUNT_KEY);
        assertThat(currentValue).isEqualTo("1000.0");
    }
}
