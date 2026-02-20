package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.module04.domain.port.outbound.CacheStampedeProtectionPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis adapter for distributed lock-based cache stampede protection.
 * Uses SET NX (setIfAbsent) with a TTL to ensure only one caller
 * rebuilds an expired cache entry while others wait or return stale data.
 */
@Component
public class RedisCacheStampedeProtectionAdapter implements CacheStampedeProtectionPort {

    private static final String KEY_PREFIX = "lock:cache:";
    private static final String LOCK_VALUE = "locked";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisCacheStampedeProtectionAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(String key, long ttlMs) {
        String lockKey = KEY_PREFIX + key;
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, LOCK_VALUE, Duration.ofMillis(ttlMs));
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void unlock(String key) {
        String lockKey = KEY_PREFIX + key;
        stringRedisTemplate.delete(lockKey);
    }
}
