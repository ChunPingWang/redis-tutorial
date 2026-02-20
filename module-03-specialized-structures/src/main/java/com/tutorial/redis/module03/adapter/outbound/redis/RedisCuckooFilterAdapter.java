package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.module03.domain.port.outbound.CuckooFilterPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis adapter for Cuckoo filter operations using Redis module commands.
 *
 * <p>Uses Lua scripts to invoke Cuckoo filter module commands
 * (CF.RESERVE, CF.ADD, CF.EXISTS, CF.DEL).</p>
 *
 * <p>Key pattern: {@code filter:cuckoo:{filterName}}</p>
 */
@Component
public class RedisCuckooFilterAdapter implements CuckooFilterPort {

    private static final String KEY_PREFIX = "filter:cuckoo";

    private static final DefaultRedisScript<Long> CF_RESERVE =
            new DefaultRedisScript<>("redis.call('CF.RESERVE', KEYS[1], ARGV[1]); return 1", Long.class);

    private static final DefaultRedisScript<Long> CF_ADD =
            new DefaultRedisScript<>("return redis.call('CF.ADD', KEYS[1], ARGV[1])", Long.class);

    private static final DefaultRedisScript<Long> CF_EXISTS =
            new DefaultRedisScript<>("return redis.call('CF.EXISTS', KEYS[1], ARGV[1])", Long.class);

    private static final DefaultRedisScript<Long> CF_DEL =
            new DefaultRedisScript<>("return redis.call('CF.DEL', KEYS[1], ARGV[1])", Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisCuckooFilterAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void createFilter(String filterName, long capacity) {
        String key = buildKey(filterName);
        redisTemplate.execute(CF_RESERVE, List.of(key), String.valueOf(capacity));
    }

    @Override
    public boolean add(String filterName, String item) {
        String key = buildKey(filterName);
        Long result = redisTemplate.execute(CF_ADD, List.of(key), item);
        return result != null && result == 1L;
    }

    @Override
    public boolean mightContain(String filterName, String item) {
        String key = buildKey(filterName);
        Long result = redisTemplate.execute(CF_EXISTS, List.of(key), item);
        return result != null && result == 1L;
    }

    @Override
    public boolean delete(String filterName, String item) {
        String key = buildKey(filterName);
        Long result = redisTemplate.execute(CF_DEL, List.of(key), item);
        return result != null && result == 1L;
    }

    private String buildKey(String filterName) {
        return KEY_PREFIX + ":" + filterName;
    }
}
