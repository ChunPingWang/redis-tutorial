package com.tutorial.redis.module02.adapter.outbound.redis;

import com.tutorial.redis.common.config.RedisKeyConvention;
import com.tutorial.redis.module02.domain.model.Transaction;
import com.tutorial.redis.module02.domain.port.outbound.TransactionLogPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Redis adapter for transaction log operations using ListOperations (Redis List).
 *
 * <p>Uses {@link RedisTemplate} with Jackson2JsonRedisSerializer because
 * Transaction objects are serialized as JSON and stored as list elements.</p>
 *
 * <p>Key pattern: {@code banking:txlog:{accountId}}<br>
 * New transactions are pushed to the head (LPUSH) for newest-first ordering.</p>
 */
@Component
public class RedisTransactionLogAdapter implements TransactionLogPort {

    private static final String SERVICE = "banking";
    private static final String ENTITY = "txlog";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTransactionLogAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addTransaction(String accountId, Transaction transaction) {
        String key = buildKey(accountId);
        redisTemplate.opsForList().leftPush(key, transaction);
    }

    @Override
    public List<Transaction> getRecentTransactions(String accountId, int count) {
        String key = buildKey(accountId);
        List<Object> values = redisTemplate.opsForList().range(key, 0, count - 1L);

        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        return values.stream()
                .filter(Transaction.class::isInstance)
                .map(Transaction.class::cast)
                .toList();
    }

    @Override
    public void trimToSize(String accountId, int maxSize) {
        String key = buildKey(accountId);
        redisTemplate.opsForList().trim(key, 0, maxSize - 1L);
    }

    @Override
    public long getTransactionCount(String accountId) {
        String key = buildKey(accountId);
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0L;
    }

    private String buildKey(String accountId) {
        return RedisKeyConvention.buildKey(SERVICE, ENTITY, accountId);
    }
}
