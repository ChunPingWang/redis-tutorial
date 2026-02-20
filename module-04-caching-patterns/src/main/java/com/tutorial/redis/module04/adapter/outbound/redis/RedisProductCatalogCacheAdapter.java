package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.common.config.RedisKeyConvention;
import com.tutorial.redis.module04.domain.model.ProductCatalog;
import com.tutorial.redis.module04.domain.port.outbound.ProductCatalogCachePort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis adapter for product catalog cache operations.
 * Supports Read-Through / Write-Through and Refresh-Ahead patterns.
 * TTL is specified per save operation to allow randomized TTL for
 * cache avalanche prevention.
 */
@Component
public class RedisProductCatalogCacheAdapter implements ProductCatalogCachePort {

    private static final String SERVICE = "cache";
    private static final String ENTITY = "product";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisProductCatalogCacheAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(ProductCatalog product, long ttlMs) {
        String key = buildKey(product.getProductId());
        redisTemplate.opsForValue().set(key, product, Duration.ofMillis(ttlMs));
    }

    @Override
    public Optional<ProductCatalog> findById(String productId) {
        String key = buildKey(productId);
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof ProductCatalog product) {
            return Optional.of(product);
        }
        return Optional.empty();
    }

    @Override
    public void evict(String productId) {
        redisTemplate.delete(buildKey(productId));
    }

    @Override
    public boolean exists(String productId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(productId)));
    }

    private String buildKey(String productId) {
        return RedisKeyConvention.buildKey(SERVICE, ENTITY, productId);
    }
}
