package com.tutorial.redis.module14.shared.adapter.outbound.redis;

import com.tutorial.redis.module14.shared.domain.port.outbound.IdempotencyPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed idempotency adapter.
 *
 * <p>Uses {@code SET NX} with TTL for atomic check-and-set idempotency
 * guarantees, ensuring that duplicate requests within the TTL window
 * are detected and short-circuited.</p>
 */
@Component
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdempotencyAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean setIfAbsent(String key, String value, long ttlSeconds) {
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(result);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }
}
