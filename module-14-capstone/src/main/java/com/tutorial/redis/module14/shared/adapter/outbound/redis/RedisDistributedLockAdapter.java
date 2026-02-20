package com.tutorial.redis.module14.shared.adapter.outbound.redis;

import com.tutorial.redis.module14.shared.domain.port.outbound.DistributedLockPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

/**
 * Redis-backed distributed lock adapter.
 *
 * <p>Uses {@code SET NX} for atomic lock acquisition and a Lua script
 * for atomic check-and-delete unlock to prevent accidental release
 * by non-owners.</p>
 */
@Component
public class RedisDistributedLockAdapter implements DistributedLockPort {

    private static final String UNLOCK_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('DEL', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisDistributedLockAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(String lockKey, String lockValue, long ttlSeconds) {
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean unlock(String lockKey, String lockValue) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(lockKey), lockValue);
        return result != null && result == 1L;
    }
}
