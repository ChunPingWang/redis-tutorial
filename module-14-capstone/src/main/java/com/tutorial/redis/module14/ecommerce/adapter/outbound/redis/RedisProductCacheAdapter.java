package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.module14.ecommerce.domain.port.outbound.ProductCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis adapter for product cache operations.
 *
 * <p>Implements {@link ProductCachePort} using Redis String operations
 * with a 30-minute TTL for cached product data.</p>
 *
 * <p>Cache key format: {@code ecommerce:product:cache:{productId}}</p>
 */
@Component
public class RedisProductCacheAdapter implements ProductCachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisProductCacheAdapter.class);
    private static final String CACHE_KEY_PREFIX = "ecommerce:product:cache:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisProductCacheAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void cacheProduct(String productId, String productJson) {
        log.debug("Caching product {} with TTL {}", productId, CACHE_TTL);
        stringRedisTemplate.opsForValue().set(CACHE_KEY_PREFIX + productId, productJson, CACHE_TTL);
    }

    @Override
    public String getCachedProduct(String productId) {
        log.debug("Retrieving cached product {}", productId);
        return stringRedisTemplate.opsForValue().get(CACHE_KEY_PREFIX + productId);
    }

    @Override
    public void evictProduct(String productId) {
        log.debug("Evicting product {} from cache", productId);
        stringRedisTemplate.delete(CACHE_KEY_PREFIX + productId);
    }
}
