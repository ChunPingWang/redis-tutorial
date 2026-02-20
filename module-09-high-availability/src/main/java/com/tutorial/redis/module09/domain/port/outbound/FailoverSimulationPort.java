package com.tutorial.redis.module09.domain.port.outbound;

/**
 * Outbound port for failover simulation operations.
 *
 * <p>Provides batch write and data integrity verification capabilities
 * used to demonstrate what happens to data during a failover event.
 * Implemented by a Redis adapter that performs bulk SET and GET operations.</p>
 */
public interface FailoverSimulationPort {

    /**
     * Writes a batch of key-value pairs to Redis using the given key prefix.
     * Keys are generated as {@code {keyPrefix}:{0}, {keyPrefix}:{1}, ...}
     *
     * @param keyPrefix the prefix for generated keys
     * @param count     the number of keys to write
     */
    void writeDataBatch(String keyPrefix, int count);

    /**
     * Verifies data integrity after a simulated failover by checking how many
     * keys with the given prefix still exist.
     *
     * @param keyPrefix     the prefix of keys to verify
     * @param expectedCount the expected total number of keys
     * @return the number of keys that were successfully read back
     */
    int verifyDataIntegrity(String keyPrefix, int expectedCount);
}
