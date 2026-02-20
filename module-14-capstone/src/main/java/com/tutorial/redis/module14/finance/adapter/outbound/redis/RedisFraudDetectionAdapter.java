package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.module14.finance.domain.port.outbound.FraudDetectionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Redis adapter for fraud detection using a Bloom filter.
 *
 * <p>Implements {@link FraudDetectionPort} using the Redis Bloom module
 * commands {@code BF.ADD} and {@code BF.EXISTS} via Lua scripts. The
 * Bloom filter provides space-efficient probabilistic membership testing
 * with a small false-positive rate and zero false negatives.</p>
 */
@Component
public class RedisFraudDetectionAdapter implements FraudDetectionPort {

    private static final Logger log = LoggerFactory.getLogger(RedisFraudDetectionAdapter.class);

    private static final String BLOOM_KEY = "finance:fraud:bloom";

    /**
     * Lua script for BF.ADD: adds an item to the Bloom filter.
     */
    private static final DefaultRedisScript<String> BF_ADD_SCRIPT = new DefaultRedisScript<>(
            "redis.call('BF.ADD', KEYS[1], ARGV[1])\n"
                    + "return 'OK'",
            String.class);

    /**
     * Lua script for BF.EXISTS: checks if an item might exist in the Bloom filter.
     * Returns "1" if might exist, "0" if definitely not.
     */
    private static final DefaultRedisScript<String> BF_EXISTS_SCRIPT = new DefaultRedisScript<>(
            "return tostring(redis.call('BF.EXISTS', KEYS[1], ARGV[1]))",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisFraudDetectionAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void addToBloomFilter(String txId) {
        List<String> keys = Collections.singletonList(BLOOM_KEY);
        try {
            stringRedisTemplate.execute(BF_ADD_SCRIPT, keys, txId);
            log.debug("Added transaction {} to Bloom filter", txId);
        } catch (Exception e) {
            log.warn("BF.ADD failed for transaction {}: {}", txId, e.getMessage());
        }
    }

    @Override
    public boolean mightExist(String txId) {
        List<String> keys = Collections.singletonList(BLOOM_KEY);
        try {
            String result = stringRedisTemplate.execute(BF_EXISTS_SCRIPT, keys, txId);
            boolean exists = "1".equals(result);
            log.debug("BF.EXISTS for transaction {}: {}", txId, exists);
            return exists;
        } catch (Exception e) {
            log.warn("BF.EXISTS failed for transaction {}: {}", txId, e.getMessage());
            return false;
        }
    }
}
