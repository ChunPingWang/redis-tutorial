package com.tutorial.redis.module08.domain.port.outbound;

/**
 * Outbound port for data recovery testing operations.
 *
 * <p>Provides the ability to write test data, count surviving keys after
 * a simulated restart, and flush all data. Implemented by a Redis adapter
 * that uses basic SET / KEYS / FLUSHALL commands.</p>
 */
public interface DataRecoveryPort {

    /**
     * Writes a batch of test key-value pairs with the given prefix.
     * Keys are named {@code {keyPrefix}:{index}} where index ranges from 0 to count-1.
     *
     * @param keyPrefix the prefix for all test keys
     * @param count     the number of keys to write
     */
    void writeTestData(String keyPrefix, int count);

    /**
     * Counts the number of existing keys that match the given prefix pattern.
     *
     * @param keyPrefix the prefix to match (uses {@code {keyPrefix}:*} pattern)
     * @return the number of matching keys found
     */
    int countKeys(String keyPrefix);

    /**
     * Flushes all data from the Redis instance (FLUSHALL command).
     */
    void flushAll();
}
