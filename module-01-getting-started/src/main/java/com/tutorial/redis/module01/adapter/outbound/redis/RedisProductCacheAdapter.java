package com.tutorial.redis.module01.adapter.outbound.redis;

import com.tutorial.redis.common.config.RedisKeyConvention;
import com.tutorial.redis.module01.domain.model.Product;
import com.tutorial.redis.module01.domain.port.outbound.ProductCachePort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class RedisProductCacheAdapter implements ProductCachePort {

    private static final String SERVICE = "ecommerce";
    private static final String ENTITY = "product";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisProductCacheAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(Product product, Duration ttl) {
        String key = buildKey(product.getProductId());
        redisTemplate.opsForValue().set(key, product, ttl);
    }

    @Override
    public Optional<Product> findById(String productId) {
        String key = buildKey(productId);
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Product product) {
            return Optional.of(product);
        }
        return Optional.empty();
    }

    @Override
    public void evict(String productId) {
        redisTemplate.delete(buildKey(productId));
    }

    @Override
    public List<Product> findByIds(List<String> productIds) {
        List<String> keys = productIds.stream()
                .map(this::buildKey)
                .toList();
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) return List.of();
        return values.stream()
                .filter(Objects::nonNull)
                .filter(Product.class::isInstance)
                .map(Product.class::cast)
                .toList();
    }

    private String buildKey(String productId) {
        return RedisKeyConvention.buildKey(SERVICE, ENTITY, productId);
    }
}
