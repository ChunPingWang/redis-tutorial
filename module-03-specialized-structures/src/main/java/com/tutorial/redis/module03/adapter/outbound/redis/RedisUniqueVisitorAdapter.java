package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.module03.domain.port.outbound.UniqueVisitorPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis adapter for unique visitor counting using HyperLogLog operations.
 *
 * <p>Uses {@link StringRedisTemplate} with {@code opsForHyperLogLog()} for
 * probabilistic cardinality estimation with very low memory usage.</p>
 *
 * <p>Key pattern: {@code analytics:uv:{pageId}:{period}}</p>
 */
@Component
public class RedisUniqueVisitorAdapter implements UniqueVisitorPort {

    private static final String KEY_PREFIX = "analytics:uv";

    private final StringRedisTemplate redisTemplate;

    public RedisUniqueVisitorAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean addVisitor(String pageId, String period, String visitorId) {
        String key = buildKey(pageId, period);
        Long result = redisTemplate.opsForHyperLogLog().add(key, visitorId);
        return result != null && result > 0;
    }

    @Override
    public long countVisitors(String pageId, String period) {
        String key = buildKey(pageId, period);
        Long count = redisTemplate.opsForHyperLogLog().size(key);
        return count != null ? count : 0L;
    }

    @Override
    public long countMergedVisitors(String destKey, List<String> sourceKeys) {
        if (sourceKeys == null || sourceKeys.isEmpty()) {
            return 0L;
        }

        String[] sources = sourceKeys.toArray(new String[0]);
        redisTemplate.opsForHyperLogLog().union(destKey, sources);

        Long count = redisTemplate.opsForHyperLogLog().size(destKey);
        return count != null ? count : 0L;
    }

    private String buildKey(String pageId, String period) {
        return KEY_PREFIX + ":" + pageId + ":" + period;
    }
}
