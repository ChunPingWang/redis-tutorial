package com.tutorial.redis.module14.shared.application.usecase;

import com.tutorial.redis.module14.shared.domain.port.outbound.DistributedLockPort;
import org.springframework.stereotype.Service;

/**
 * Application service for distributed lock management.
 *
 * <p>Provides a higher-level API over the {@link DistributedLockPort},
 * automatically prefixing lock keys with {@code lock:} to namespace
 * them within the Redis keyspace.</p>
 */
@Service
public class DistributedLockService {

    private final DistributedLockPort distributedLockPort;

    public DistributedLockService(DistributedLockPort distributedLockPort) {
        this.distributedLockPort = distributedLockPort;
    }

    /**
     * Acquires a distributed lock for the given resource.
     *
     * @param resource   the resource name to lock
     * @param owner      the identifier of the lock owner
     * @param ttlSeconds the lock's time-to-live in seconds
     * @return {@code true} if the lock was acquired, {@code false} otherwise
     */
    public boolean acquireLock(String resource, String owner, long ttlSeconds) {
        return distributedLockPort.tryLock("lock:" + resource, owner, ttlSeconds);
    }

    /**
     * Releases a distributed lock for the given resource.
     *
     * @param resource the resource name to unlock
     * @param owner    the identifier of the lock owner
     * @return {@code true} if the lock was released, {@code false} otherwise
     */
    public boolean releaseLock(String resource, String owner) {
        return distributedLockPort.unlock("lock:" + resource, owner);
    }
}
