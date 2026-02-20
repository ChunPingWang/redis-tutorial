package com.tutorial.redis.module08.adapter.outbound.redis;

import com.tutorial.redis.module08.domain.port.outbound.DataRecoveryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Redis adapter that implements {@link DataRecoveryPort} for data recovery
 * testing operations.
 *
 * <p>Provides the ability to write batches of test keys, count surviving keys
 * by pattern, and flush all data. Uses {@link StringRedisTemplate} for all
 * operations.</p>
 *
 * <p><strong>Note:</strong> The {@link #countKeys(String)} method uses the
 * {@code KEYS} command which scans the entire keyspace. This is acceptable
 * for testing and tutorial purposes but should never be used in production
 * environments where {@code SCAN} should be preferred.</p>
 */
@Component
public class RedisDataRecoveryAdapter implements DataRecoveryPort {

    private static final Logger log = LoggerFactory.getLogger(RedisDataRecoveryAdapter.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisDataRecoveryAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Writes a batch of test key-value pairs with the given prefix using
     * pipelining for performance.
     *
     * <p>Keys are named {@code {keyPrefix}:{index}} where index ranges from
     * 0 to count-1. Values follow the pattern {@code value-{index}}.</p>
     *
     * @param keyPrefix the prefix for all test keys
     * @param count     the number of keys to write
     */
    @Override
    public void writeTestData(String keyPrefix, int count) {
        log.info("Writing {} test keys with prefix '{}'", count, keyPrefix);

        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (int i = 0; i < count; i++) {
                byte[] key = (keyPrefix + ":" + i).getBytes(StandardCharsets.UTF_8);
                byte[] value = ("value-" + i).getBytes(StandardCharsets.UTF_8);
                connection.stringCommands().set(key, value);
            }
            // RedisCallback must return null when used with executePipelined
            return null;
        });

        log.info("Successfully wrote {} test keys with prefix '{}'", count, keyPrefix);
    }

    /**
     * Counts the number of existing keys that match the given prefix pattern.
     *
     * <p>Uses the {@code KEYS {keyPrefix}:*} command to find all matching keys
     * and returns the set size. This is acceptable for testing purposes only.</p>
     *
     * @param keyPrefix the prefix to match (uses {@code {keyPrefix}:*} pattern)
     * @return the number of matching keys found
     */
    @Override
    public int countKeys(String keyPrefix) {
        String pattern = keyPrefix + ":*";
        log.debug("Counting keys matching pattern '{}'", pattern);

        Set<String> keys = stringRedisTemplate.keys(pattern);
        int count = (keys != null) ? keys.size() : 0;

        log.debug("Found {} keys matching pattern '{}'", count, pattern);
        return count;
    }

    /**
     * Flushes all data from the Redis instance using the {@code FLUSHALL} command.
     */
    @Override
    public void flushAll() {
        log.warn("Executing FLUSHALL — all data in the Redis instance will be deleted");
        stringRedisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
        log.info("FLUSHALL completed successfully");
    }
}
