package com.tutorial.redis.module05.adapter.outbound.redis;

import com.tutorial.redis.module05.domain.model.TransferResult;
import com.tutorial.redis.module05.domain.port.outbound.TransactionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis adapter implementing atomic money transfer using MULTI/EXEC transactions.
 * Uses WATCH for optimistic locking to detect concurrent modifications,
 * and MULTI/EXEC to guarantee atomic execution of the debit and credit.
 *
 * <p>Key format: {@code account:balance:{accountId}}</p>
 */
@Component
public class RedisTransactionAdapter implements TransactionPort {

    private static final Logger log = LoggerFactory.getLogger(RedisTransactionAdapter.class);
    private static final String KEY_PREFIX = "account:balance:";

    private final StringRedisTemplate redisTemplate;

    public RedisTransactionAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically transfers an amount from one account to another.
     * <ol>
     *   <li>WATCH both account keys to detect concurrent modifications</li>
     *   <li>GET both balances and validate sufficient funds</li>
     *   <li>MULTI to start the transaction block</li>
     *   <li>SET the new balances for both accounts</li>
     *   <li>EXEC to commit atomically</li>
     * </ol>
     * If EXEC returns null (WATCH detected a change), the transfer is aborted
     * with a "Concurrent modification" message.
     */
    @Override
    public TransferResult transfer(String fromAccountId, String toAccountId, double amount) {
        log.debug("Transferring {} from account {} to account {}", amount, fromAccountId, toAccountId);

        String fromKey = buildKey(fromAccountId);
        String toKey = buildKey(toAccountId);

        List<Object> txResults = redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                // Step 1: WATCH both keys for concurrent modification detection
                operations.watch(List.of(fromKey, toKey));

                // Step 2: GET current balances
                String fromBalStr = (String) operations.opsForValue().get(fromKey);
                String toBalStr = (String) operations.opsForValue().get(toKey);

                double fromBalance = fromBalStr != null ? Double.parseDouble(fromBalStr) : 0.0;
                double toBalance = toBalStr != null ? Double.parseDouble(toBalStr) : 0.0;

                // Step 3: Check sufficient funds
                if (fromBalance < amount) {
                    operations.unwatch();
                    return null;
                }

                // Step 4: MULTI — start transaction
                operations.multi();

                // Step 5: SET new balances
                double newFromBalance = fromBalance - amount;
                double newToBalance = toBalance + amount;
                operations.opsForValue().set(fromKey, String.valueOf(newFromBalance));
                operations.opsForValue().set(toKey, String.valueOf(newToBalance));

                // Step 6: EXEC — commit atomically
                return operations.exec();
            }
        });

        if (txResults == null) {
            // Either insufficient balance or concurrent modification not distinguishable here.
            // We check explicitly: if WATCH was unwatched due to insufficient balance,
            // txResults is null from our explicit return; if EXEC returned null, it's concurrent mod.
            // To distinguish, we re-read the balance.
            String fromBalStr = redisTemplate.opsForValue().get(fromKey);
            double currentBalance = fromBalStr != null ? Double.parseDouble(fromBalStr) : 0.0;
            if (currentBalance < amount) {
                log.debug("Transfer failed: insufficient balance in account {}", fromAccountId);
                return new TransferResult(fromAccountId, toAccountId, amount, false, "Insufficient balance");
            }
            log.debug("Transfer failed: concurrent modification detected");
            return new TransferResult(fromAccountId, toAccountId, amount, false, "Concurrent modification");
        }

        log.debug("Transfer succeeded: {} from {} to {}", amount, fromAccountId, toAccountId);
        return new TransferResult(fromAccountId, toAccountId, amount, true, "Transfer successful");
    }

    private String buildKey(String accountId) {
        return KEY_PREFIX + accountId;
    }
}
