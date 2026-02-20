package com.tutorial.redis.module14.shared.domain.port.outbound;

/**
 * Outbound port for distributed unique ID generation.
 *
 * <p>Provides atomic counter increments via Redis to generate
 * monotonically increasing sequence numbers for unique ID composition.</p>
 */
public interface UniqueIdPort {

    /**
     * Returns the next sequence number for the given counter key.
     *
     * @param counterKey the Redis key for the sequence counter
     * @return the next sequence number (monotonically increasing)
     */
    long nextSequence(String counterKey);
}
