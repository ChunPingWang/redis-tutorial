package com.tutorial.redis.module02.adapter.outbound.redis;

import com.tutorial.redis.common.config.RedisKeyConvention;
import com.tutorial.redis.module02.domain.port.outbound.StockLevelPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Redis adapter for stock level operations using ValueOperations (Redis String).
 *
 * <p>Uses {@link StringRedisTemplate} because stock levels are numeric values
 * stored as strings — ideal for Redis INCR/DECR atomic counter operations.</p>
 *
 * <p>Key pattern: {@code ecommerce:stock:{productId}}</p>
 */
@Component
public class RedisStockLevelAdapter implements StockLevelPort {

    private static final String SERVICE = "ecommerce";
    private static final String ENTITY = "stock";

    private final StringRedisTemplate redisTemplate;

    public RedisStockLevelAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setLevel(String productId, long quantity) {
        String key = buildKey(productId);
        redisTemplate.opsForValue().set(key, String.valueOf(quantity));
    }

    @Override
    public long increment(String productId, long delta) {
        String key = buildKey(productId);
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result != null ? result : 0L;
    }

    @Override
    public long decrement(String productId, long delta) {
        String key = buildKey(productId);
        Long result = redisTemplate.opsForValue().increment(key, -delta);
        return result != null ? result : 0L;
    }

    @Override
    public OptionalLong getLevel(String productId) {
        String key = buildKey(productId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Long.parseLong(value));
    }

    @Override
    public Map<String, Long> batchGetLevels(List<String> productIds) {
        List<String> keys = productIds.stream()
                .map(this::buildKey)
                .toList();

        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        Map<String, Long> result = new HashMap<>();

        if (values != null) {
            for (int i = 0; i < productIds.size(); i++) {
                String value = values.get(i);
                if (value != null) {
                    result.put(productIds.get(i), Long.parseLong(value));
                }
            }
        }

        return result;
    }

    private String buildKey(String productId) {
        return RedisKeyConvention.buildKey(SERVICE, ENTITY, productId);
    }
}
