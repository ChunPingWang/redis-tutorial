package com.tutorial.redis.module14.shared.adapter.outbound.redis;

import com.tutorial.redis.module14.shared.domain.port.outbound.UniqueIdPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed unique ID sequence adapter.
 *
 * <p>Uses Redis {@code INCR} for atomic, monotonically increasing
 * sequence number generation, suitable for distributed unique ID
 * composition across multiple application instances.</p>
 */
@Component
public class RedisUniqueIdAdapter implements UniqueIdPort {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisUniqueIdAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public long nextSequence(String counterKey) {
        Long result = stringRedisTemplate.opsForValue().increment(counterKey);
        return result != null ? result : 0L;
    }
}
