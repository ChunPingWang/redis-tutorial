package com.tutorial.redis.module04.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheEntry 領域模型測試")
class CacheEntryTest {

    @Test
    @DisplayName("isExpired_WhenExpired_ReturnsTrue — 超過 TTL 時判定為已過期")
    void isExpired_WhenExpired_ReturnsTrue() {
        Instant createdAt = Instant.now().minusMillis(10_000);
        CacheEntry<String> entry = new CacheEntry<>("value", createdAt, 5_000);

        // 10 seconds have elapsed, TTL is 5 seconds => expired
        boolean expired = entry.isExpired(Instant.now());

        assertThat(expired).isTrue();
    }

    @Test
    @DisplayName("isExpired_WhenNotExpired_ReturnsFalse — 未超過 TTL 時判定為未過期")
    void isExpired_WhenNotExpired_ReturnsFalse() {
        Instant createdAt = Instant.now().minusMillis(1_000);
        CacheEntry<String> entry = new CacheEntry<>("value", createdAt, 60_000);

        // 1 second has elapsed, TTL is 60 seconds => not expired
        boolean expired = entry.isExpired(Instant.now());

        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("remainingTtlMs_ReturnsCorrectValue — 正確計算剩餘 TTL 毫秒數")
    void remainingTtlMs_ReturnsCorrectValue() {
        Instant createdAt = Instant.now().minusMillis(3_000);
        CacheEntry<String> entry = new CacheEntry<>("value", createdAt, 10_000);

        // 3 seconds elapsed, TTL is 10 seconds => ~7 seconds remaining
        long remaining = entry.remainingTtlMs(Instant.now());

        assertThat(remaining).isGreaterThan(6_000);
        assertThat(remaining).isLessThanOrEqualTo(7_100); // small tolerance for test execution time
    }
}
