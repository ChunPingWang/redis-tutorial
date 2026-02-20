package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.module04.domain.model.TransactionEvent;
import com.tutorial.redis.module04.domain.port.outbound.TransactionBufferPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis adapter for transaction event buffering using a Redis List.
 * Supports the Write-Behind (Write-Back) pattern where events are
 * buffered via RPUSH and drained via LPOP for batch persistence.
 */
@Component
public class RedisTransactionBufferAdapter implements TransactionBufferPort {

    private static final String BUFFER_KEY = "buffer:transaction";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTransactionBufferAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void buffer(TransactionEvent event) {
        redisTemplate.opsForList().rightPush(BUFFER_KEY, event);
    }

    @Override
    public List<TransactionEvent> drainBatch(int batchSize) {
        List<TransactionEvent> batch = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            Object value = redisTemplate.opsForList().leftPop(BUFFER_KEY);
            if (value == null) {
                break;
            }
            if (value instanceof TransactionEvent event) {
                batch.add(event);
            }
        }
        return batch;
    }

    @Override
    public long size() {
        Long length = redisTemplate.opsForList().size(BUFFER_KEY);
        return length != null ? length : 0L;
    }
}
