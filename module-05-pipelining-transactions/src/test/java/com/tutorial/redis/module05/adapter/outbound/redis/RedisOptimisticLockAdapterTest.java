package com.tutorial.redis.module05.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisOptimisticLockAdapter 整合測試")
class RedisOptimisticLockAdapterTest extends AbstractRedisIntegrationTest {

    private static final String ACCOUNT_KEY = "account:balance:ACC-001";

    @Autowired
    private RedisOptimisticLockAdapter adapter;

    @BeforeEach
    void setUpAccount() {
        stringRedisTemplate.opsForValue().set(ACCOUNT_KEY, "1000.0");
    }

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
