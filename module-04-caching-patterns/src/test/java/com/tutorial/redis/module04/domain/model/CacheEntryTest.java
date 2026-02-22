package com.tutorial.redis.module04.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 快取條目領域模型測試。
 * 驗證 CacheEntry 的 TTL 過期判斷與剩餘存活時間計算邏輯。
 * 此模型為 TTL 策略的核心，支援快取自動過期與 Refresh-Ahead 判斷。
 * 屬於 Domain 層（領域模型）。
 */
@DisplayName("CacheEntry 領域模型測試")
class CacheEntryTest {

    // 驗證經過時間超過 TTL 時，isExpired 回傳 true
    @Test
    @DisplayName("isExpired_WhenExpired_ReturnsTrue — 超過 TTL 時判定為已過期")
    void isExpired_WhenExpired_ReturnsTrue() {
        Instant createdAt = Instant.now().minusMillis(10_000);
        CacheEntry<String> entry = new CacheEntry<>("value", createdAt, 5_000);

        // 10 seconds have elapsed, TTL is 5 seconds => expired
        boolean expired = entry.isExpired(Instant.now());

        assertThat(expired).isTrue();
    }

    // 驗證經過時間未超過 TTL 時，isExpired 回傳 false
    @Test
    @DisplayName("isExpired_WhenNotExpired_ReturnsFalse — 未超過 TTL 時判定為未過期")
    void isExpired_WhenNotExpired_ReturnsFalse() {
        Instant createdAt = Instant.now().minusMillis(1_000);
        CacheEntry<String> entry = new CacheEntry<>("value", createdAt, 60_000);

        // 1 second has elapsed, TTL is 60 seconds => not expired
        boolean expired = entry.isExpired(Instant.now());

        assertThat(expired).isFalse();
    }

    // 驗證 remainingTtlMs 正確計算剩餘存活時間（毫秒）
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
