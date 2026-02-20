package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module14.ecommerce.domain.model.RateLimitResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisRateLimiterAdapter 整合測試")
class RedisRateLimiterAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisRateLimiterAdapter adapter;

    @Test
    @DisplayName("tryAcquire_UnderLimit_Allowed — 未超過限制時應允許請求")
    void tryAcquire_UnderLimit_Allowed() {
        // Act
        RateLimitResult result = adapter.tryAcquire("ecommerce:ratelimit:test-client", 5, 60);

        // Assert
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemainingTokens()).isEqualTo(4);
        assertThat(result.getRetryAfterMs()).isEqualTo(0);
    }

    @Test
    @DisplayName("tryAcquire_OverLimit_Denied — 超過限制時應拒絕請求")
    void tryAcquire_OverLimit_Denied() {
        // Arrange — exhaust the rate limit
        String key = "ecommerce:ratelimit:test-client-exhausted";
        int maxRequests = 3;
        for (int i = 0; i < maxRequests; i++) {
            adapter.tryAcquire(key, maxRequests, 60);
        }

        // Act — one more request should be denied
        RateLimitResult result = adapter.tryAcquire(key, maxRequests, 60);

        // Assert
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemainingTokens()).isEqualTo(0);
        assertThat(result.getRetryAfterMs()).isGreaterThan(0);
    }
}
