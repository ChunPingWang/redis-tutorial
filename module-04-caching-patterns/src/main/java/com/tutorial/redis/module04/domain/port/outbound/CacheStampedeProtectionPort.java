package com.tutorial.redis.module04.domain.port.outbound;

/**
 * Outbound port for distributed locking used in cache stampede protection.
 * When a hot cache key expires, many concurrent requests may simultaneously
 * attempt to rebuild it, causing a "stampede" on the data source.
 * This port provides a distributed lock so that only one caller rebuilds
 * the cache while others wait or return stale data.
 * Implemented by a Redis adapter (e.g. SET NX PX) in the infrastructure layer.
 */
public interface CacheStampedeProtectionPort {

    /**
     * Attempts to acquire a distributed lock for the given key.
     *
     * @param key   the lock key (typically derived from the cache key)
     * @param ttlMs lock auto-release time in milliseconds
     * @return true if the lock was acquired, false if another caller holds it
     */
    boolean tryLock(String key, long ttlMs);

    /**
     * Releases the distributed lock for the given key.
     *
     * @param key the lock key
     */
    void unlock(String key);
}
