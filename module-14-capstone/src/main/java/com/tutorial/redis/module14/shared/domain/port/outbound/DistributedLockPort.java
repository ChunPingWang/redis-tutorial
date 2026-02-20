package com.tutorial.redis.module14.shared.domain.port.outbound;

/**
 * Outbound port for distributed lock operations.
 *
 * <p>Provides atomic lock acquisition and release using Redis as the
 * coordination backend. Implementations must guarantee mutual exclusion
 * and support owner-based unlock to prevent accidental release.</p>
 */
public interface DistributedLockPort {

    /**
     * Attempts to acquire a distributed lock.
     *
     * @param lockKey   the key identifying the lock
     * @param lockValue the value identifying the lock owner
     * @param ttlSeconds the lock's time-to-live in seconds
     * @return {@code true} if the lock was acquired, {@code false} otherwise
     */
    boolean tryLock(String lockKey, String lockValue, long ttlSeconds);

    /**
     * Releases a distributed lock only if the caller is the current owner.
     *
     * @param lockKey   the key identifying the lock
     * @param lockValue the value identifying the lock owner
     * @return {@code true} if the lock was released, {@code false} otherwise
     */
    boolean unlock(String lockKey, String lockValue);
}
