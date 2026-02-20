package com.tutorial.redis.module05.adapter.outbound.redis;

import com.tutorial.redis.module05.domain.port.outbound.OptimisticLockPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis adapter implementing optimistic locking via WATCH + MULTI/EXEC.
 * Provides compare-and-set (CAS) semantics for account balance updates:
 * the operation succeeds only if the current value matches the expected value
 * at the time of EXEC, otherwise it is aborted.
 *
 * <p>Key format: {@code account:balance:{accountId}}</p>
 */
@Component
public class RedisOptimisticLockAdapter implements OptimisticLockPort {

    private static final Logger log = LoggerFactory.getLogger(RedisOptimisticLockAdapter.class);
    private static final String KEY_PREFIX = "account:balance:";

    private final StringRedisTemplate redisTemplate;

    public RedisOptimisticLockAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Performs a compare-and-set update on an account balance using
     * Redis WATCH + MULTI/EXEC for optimistic locking.
     * <ol>
     *   <li>WATCH the account key</li>
     *   <li>GET the current balance</li>
     *   <li>Compare against the expected balance</li>
     *   <li>If match: MULTI, SET new balance, EXEC</li>
     *   <li>If mismatch or EXEC returns null: return false</li>
     * </ol>
     */
    @Override
    public boolean compareAndSetBalance(String accountId, double expectedBalance, double newBalance) {
        String key = buildKey(accountId);

        log.debug("CAS balance update for account {}: expected={}, new={}",
                accountId, expectedBalance, newBalance);

        List<Object> txResults = redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                // Step 1: WATCH the key for concurrent modification detection
                operations.watch(key);

                // Step 2: GET current balance
                String currentBalStr = (String) operations.opsForValue().get(key);
                double currentBalance = currentBalStr != null ? Double.parseDouble(currentBalStr) : 0.0;

                // Step 3: Compare against expected balance
                if (Double.compare(currentBalance, expectedBalance) != 0) {
                    operations.unwatch();
                    return null;
                }

                // Step 4: MULTI — start transaction
                operations.multi();

                // Step 5: SET new balance
                operations.opsForValue().set(key, String.valueOf(newBalance));

                // Step 6: EXEC — commit atomically
                return operations.exec();
            }
        });

        boolean success = txResults != null && !txResults.isEmpty();
        log.debug("CAS balance update for account {}: {}", accountId, success ? "succeeded" : "failed");
        return success;
    }

    private String buildKey(String accountId) {
        return KEY_PREFIX + accountId;
    }
}
