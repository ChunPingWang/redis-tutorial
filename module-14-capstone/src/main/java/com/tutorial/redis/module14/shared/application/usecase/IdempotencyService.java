package com.tutorial.redis.module14.shared.application.usecase;

import com.tutorial.redis.module14.shared.domain.port.outbound.IdempotencyPort;
import org.springframework.stereotype.Service;

/**
 * Application service for idempotency management.
 *
 * <p>Provides a higher-level API over the {@link IdempotencyPort},
 * automatically prefixing idempotency keys with {@code idempotency:}
 * to namespace them within the Redis keyspace.</p>
 */
@Service
public class IdempotencyService {

    private final IdempotencyPort idempotencyPort;

    public IdempotencyService(IdempotencyPort idempotencyPort) {
        this.idempotencyPort = idempotencyPort;
    }

    /**
     * Checks whether a key has been processed and sets it if not.
     *
     * @param key        the idempotency key
     * @param value      the result value to store
     * @param ttlSeconds the time-to-live in seconds
     * @return {@code true} if the key was newly set (first processing),
     *         {@code false} if already existed (duplicate)
     */
    public boolean checkAndSet(String key, String value, long ttlSeconds) {
        return idempotencyPort.setIfAbsent("idempotency:" + key, value, ttlSeconds);
    }

    /**
     * Retrieves the stored result for an idempotency key.
     *
     * @param key the idempotency key
     * @return the stored value, or {@code null} if not found
     */
    public String getResult(String key) {
        return idempotencyPort.get("idempotency:" + key);
    }
}
