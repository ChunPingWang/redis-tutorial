package com.tutorial.redis.module09.adapter.outbound.redis;

import com.tutorial.redis.module09.domain.port.outbound.FailoverSimulationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis adapter that implements {@link FailoverSimulationPort} for failover
 * simulation operations.
 *
 * <p>Provides batch write and data integrity verification capabilities used
 * to demonstrate what happens to data during a failover event. Uses
 * {@link StringRedisTemplate} for all operations.</p>
 *
 * <p>The {@link #writeDataBatch(String, int)} method uses pipelining via
 * {@code executePipelined(RedisCallback)} for performance, sending all SET
 * commands in a single round trip. The {@link #verifyDataIntegrity(String, int)}
 * method iterates over the expected key range and counts how many keys
 * still exist after a failover.</p>
 */
@Component
public class RedisFailoverSimulationAdapter implements FailoverSimulationPort {

    private static final Logger log = LoggerFactory.getLogger(RedisFailoverSimulationAdapter.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisFailoverSimulationAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Writes a batch of key-value pairs to Redis using pipelining for performance.
     *
     * <p>Keys are named {@code {keyPrefix}:{index}} where index ranges from
     * 0 to count-1. Values follow the pattern {@code value-{index}}.</p>
     *
     * <p>All SET commands are sent in a single pipeline to minimise network
     * round trips, which is especially important when simulating bulk writes
     * before a failover.</p>
     *
     * @param keyPrefix the prefix for generated keys
     * @param count     the number of keys to write
     */
    @Override
    public void writeDataBatch(String keyPrefix, int count) {
        log.info("Writing {} keys with prefix '{}' using pipeline", count, keyPrefix);

        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (int i = 0; i < count; i++) {
                byte[] key = (keyPrefix + ":" + i).getBytes(StandardCharsets.UTF_8);
                byte[] value = ("value-" + i).getBytes(StandardCharsets.UTF_8);
                connection.stringCommands().set(key, value);
            }
            // RedisCallback must return null when used with executePipelined
            return null;
        });

        log.info("Successfully wrote {} keys with prefix '{}'", count, keyPrefix);
    }

    /**
     * Verifies data integrity after a simulated failover by checking how many
     * keys with the given prefix still exist.
     *
     * <p>Iterates over keys {@code {keyPrefix}:0} through
     * {@code {keyPrefix}:{expectedCount-1}} and counts how many are present
     * in the current Redis instance. A count lower than {@code expectedCount}
     * indicates data loss during the failover.</p>
     *
     * @param keyPrefix     the prefix of keys to verify
     * @param expectedCount the expected total number of keys
     * @return the number of keys that were successfully found
     */
    @Override
    public int verifyDataIntegrity(String keyPrefix, int expectedCount) {
        log.info("Verifying data integrity for prefix '{}', expecting {} keys", keyPrefix, expectedCount);

        int foundCount = 0;
        for (int i = 0; i < expectedCount; i++) {
            String key = keyPrefix + ":" + i;
            Boolean exists = stringRedisTemplate.hasKey(key);
            if (Boolean.TRUE.equals(exists)) {
                foundCount++;
            }
        }

        log.info("Data integrity check: found {}/{} keys for prefix '{}'",
                foundCount, expectedCount, keyPrefix);
        return foundCount;
    }
}
