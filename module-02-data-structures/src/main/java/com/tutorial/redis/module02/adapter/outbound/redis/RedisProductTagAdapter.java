package com.tutorial.redis.module02.adapter.outbound.redis;

import com.tutorial.redis.common.config.RedisKeyConvention;
import com.tutorial.redis.module02.domain.port.outbound.ProductTagPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * Redis adapter for product tag operations using SetOperations (Redis Set).
 *
 * <p>Uses {@link StringRedisTemplate} because tags are plain strings —
 * no JSON serialization needed.</p>
 *
 * <p>Key pattern: {@code ecommerce:tags:{productId}}<br>
 * Supports Redis set operations: SADD, SREM, SMEMBERS, SISMEMBER,
 * SINTER (common tags), SUNION (all tags), SDIFF (unique tags).</p>
 */
@Component
public class RedisProductTagAdapter implements ProductTagPort {

    private static final String SERVICE = "ecommerce";
    private static final String ENTITY = "tags";

    private final StringRedisTemplate redisTemplate;

    public RedisProductTagAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addTags(String productId, Set<String> tags) {
        String key = buildKey(productId);
        redisTemplate.opsForSet().add(key, tags.toArray(new String[0]));
    }

    @Override
    public void removeTags(String productId, Set<String> tags) {
        String key = buildKey(productId);
        redisTemplate.opsForSet().remove(key, tags.toArray(new Object[0]));
    }

    @Override
    public Set<String> getTags(String productId) {
        String key = buildKey(productId);
        Set<String> tags = redisTemplate.opsForSet().members(key);
        return tags != null ? tags : Collections.emptySet();
    }

    @Override
    public boolean hasTag(String productId, String tag) {
        String key = buildKey(productId);
        Boolean result = redisTemplate.opsForSet().isMember(key, tag);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public Set<String> getCommonTags(String productId1, String productId2) {
        String key1 = buildKey(productId1);
        String key2 = buildKey(productId2);
        Set<String> result = redisTemplate.opsForSet().intersect(key1, key2);
        return result != null ? result : Collections.emptySet();
    }

    @Override
    public Set<String> getAllTags(String productId1, String productId2) {
        String key1 = buildKey(productId1);
        String key2 = buildKey(productId2);
        Set<String> result = redisTemplate.opsForSet().union(key1, key2);
        return result != null ? result : Collections.emptySet();
    }

    @Override
    public Set<String> getUniqueTags(String productId1, String productId2) {
        String key1 = buildKey(productId1);
        String key2 = buildKey(productId2);
        Set<String> result = redisTemplate.opsForSet().difference(key1, key2);
        return result != null ? result : Collections.emptySet();
    }

    private String buildKey(String productId) {
        return RedisKeyConvention.buildKey(SERVICE, ENTITY, productId);
    }
}
