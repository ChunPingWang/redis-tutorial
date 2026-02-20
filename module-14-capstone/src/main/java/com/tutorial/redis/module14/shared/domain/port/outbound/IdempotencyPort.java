package com.tutorial.redis.module14.shared.domain.port.outbound;

/**
 * Outbound port for idempotency operations.
 *
 * <p>Provides set-if-absent semantics backed by Redis to ensure that
 * operations are processed at most once within the specified TTL window.</p>
 */
public interface IdempotencyPort {

    /**
     * Sets a key-value pair only if the key does not already exist.
     *
     * @param key        the idempotency key
     * @param value      the result value to store
     * @param ttlSeconds the time-to-live in seconds
     * @return {@code true} if the key was set (first occurrence),
     *         {@code false} if it already existed
     */
    boolean setIfAbsent(String key, String value, long ttlSeconds);

    /**
     * Retrieves the stored result for an idempotency key.
     *
     * @param key the idempotency key
     * @return the stored value, or {@code null} if not found
     */
    String get(String key);
}
