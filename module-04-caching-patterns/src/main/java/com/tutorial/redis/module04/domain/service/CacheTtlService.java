package com.tutorial.redis.module04.domain.service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Domain service for cache TTL calculations.
 * Pure domain logic — zero framework dependency.
 *
 * <p>Provides two utilities used across caching patterns:</p>
 * <ul>
 *   <li><b>TTL randomization</b> — spreads expiration times to prevent
 *       cache avalanche (mass simultaneous expiry).</li>
 *   <li><b>Refresh-ahead detection</b> — determines whether a cache entry
 *       should be proactively refreshed before it expires.</li>
 * </ul>
 */
public class CacheTtlService {

    private static final double REFRESH_THRESHOLD = 0.20;

    /**
     * Randomizes a base TTL by adding a uniformly-distributed random amount
     * in the range {@code [0, baseTtlMs * spreadFactor)}.
     * This prevents cache avalanche by ensuring that entries created around
     * the same time do not all expire at the exact same instant.
     *
     * <p>Example: {@code randomizeTtl(60_000, 0.2)} returns a value in
     * the range [60000, 72000).</p>
     *
     * @param baseTtlMs    the base time-to-live in milliseconds (must be positive)
     * @param spreadFactor the spread factor (must be non-negative, typically 0.1–0.5)
     * @return a randomized TTL in milliseconds
     * @throws IllegalArgumentException if baseTtlMs is not positive or spreadFactor is negative
     */
    public long randomizeTtl(long baseTtlMs, double spreadFactor) {
        if (baseTtlMs <= 0) {
            throw new IllegalArgumentException("baseTtlMs must be positive");
        }
        if (spreadFactor < 0) {
            throw new IllegalArgumentException("spreadFactor must not be negative");
        }
        if (spreadFactor == 0) {
            return baseTtlMs;
        }
        long spread = (long) (baseTtlMs * spreadFactor);
        long randomOffset = ThreadLocalRandom.current().nextLong(spread);
        return baseTtlMs + randomOffset;
    }

    /**
     * Determines whether a cache entry should be proactively refreshed.
     * Returns {@code true} when the remaining TTL is less than 20% of
     * the original TTL, indicating the entry is about to expire and
     * should be reloaded asynchronously.
     *
     * <p>Example: if the original TTL is 60s and 50s have elapsed
     * (remaining = 10s), this returns {@code true} because
     * 10s / 60s = 16.7% which is below 20%.</p>
     *
     * @param remainingTtlMs the remaining time-to-live in milliseconds
     * @param originalTtlMs  the original time-to-live in milliseconds (must be positive)
     * @return true if the entry should be refreshed
     * @throws IllegalArgumentException if originalTtlMs is not positive
     */
    public boolean shouldRefresh(long remainingTtlMs, long originalTtlMs) {
        if (originalTtlMs <= 0) {
            throw new IllegalArgumentException("originalTtlMs must be positive");
        }
        if (remainingTtlMs <= 0) {
            return true;
        }
        double ratio = (double) remainingTtlMs / originalTtlMs;
        return ratio < REFRESH_THRESHOLD;
    }
}
