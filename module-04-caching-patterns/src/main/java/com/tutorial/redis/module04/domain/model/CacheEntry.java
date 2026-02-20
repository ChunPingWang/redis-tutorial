package com.tutorial.redis.module04.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Generic cache wrapper that records when a value was cached and its TTL.
 * Used for the Refresh-Ahead pattern to determine whether the remaining
 * TTL has fallen below a threshold (e.g. 20% of the original) and the
 * cache entry should be proactively refreshed before it expires.
 * Immutable value object — all fields are final.
 *
 * @param <T> the type of the cached value
 */
public class CacheEntry<T> {

    private final T value;
    private final Instant createdAt;
    private final long ttlMs;

    public CacheEntry(T value, Instant createdAt, long ttlMs) {
        this.value = Objects.requireNonNull(value, "value must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (ttlMs <= 0) {
            throw new IllegalArgumentException("ttlMs must be positive");
        }
        this.ttlMs = ttlMs;
    }

    public T getValue() { return value; }
    public Instant getCreatedAt() { return createdAt; }
    public long getTtlMs() { return ttlMs; }

    /**
     * Calculates the remaining TTL in milliseconds relative to the given instant.
     * Returns zero if the entry has already expired.
     */
    public long remainingTtlMs(Instant now) {
        long elapsed = now.toEpochMilli() - createdAt.toEpochMilli();
        return Math.max(0, ttlMs - elapsed);
    }

    /**
     * Checks whether this cache entry has expired relative to the given instant.
     */
    public boolean isExpired(Instant now) {
        return remainingTtlMs(now) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheEntry<?> that)) return false;
        return ttlMs == that.ttlMs
                && value.equals(that.value)
                && createdAt.equals(that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, createdAt, ttlMs);
    }

    @Override
    public String toString() {
        return "CacheEntry{value=%s, createdAt=%s, ttlMs=%d}".formatted(value, createdAt, ttlMs);
    }
}
