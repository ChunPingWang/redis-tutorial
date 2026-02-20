package com.tutorial.redis.module02.adapter.outbound.redis;

import com.tutorial.redis.common.config.RedisKeyConvention;
import com.tutorial.redis.module02.domain.model.CartItem;
import com.tutorial.redis.module02.domain.model.ShoppingCart;
import com.tutorial.redis.module02.domain.port.outbound.ShoppingCartPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Redis adapter for shopping cart operations using HashOperations (Redis Hash).
 *
 * <p>Uses {@link RedisTemplate} with Jackson2JsonRedisSerializer because
 * CartItem values are serialized as JSON objects in hash fields.</p>
 *
 * <p>Key pattern: {@code ecommerce:cart:{customerId}}<br>
 * Hash field = productId, Hash value = CartItem (JSON serialized)</p>
 */
@Component
public class RedisShoppingCartAdapter implements ShoppingCartPort {

    private static final String SERVICE = "ecommerce";
    private static final String ENTITY = "cart";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisShoppingCartAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addItem(String customerId, CartItem item) {
        String key = buildKey(customerId);
        redisTemplate.opsForHash().put(key, item.getProductId(), item);
    }

    @Override
    public void removeItem(String customerId, String productId) {
        String key = buildKey(customerId);
        redisTemplate.opsForHash().delete(key, productId);
    }

    @Override
    public void updateItemQuantity(String customerId, String productId, int quantity) {
        String key = buildKey(customerId);
        Object existing = redisTemplate.opsForHash().get(key, productId);
        if (existing instanceof CartItem cartItem) {
            CartItem updated = cartItem.withQuantity(quantity);
            redisTemplate.opsForHash().put(key, productId, updated);
        }
    }

    @Override
    public Optional<CartItem> getItem(String customerId, String productId) {
        String key = buildKey(customerId);
        Object value = redisTemplate.opsForHash().get(key, productId);
        if (value instanceof CartItem cartItem) {
            return Optional.of(cartItem);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ShoppingCart> getCart(String customerId) {
        String key = buildKey(customerId);
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            return Optional.empty();
        }

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, CartItem> items = new LinkedHashMap<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String productId = (String) entry.getKey();
            if (entry.getValue() instanceof CartItem cartItem) {
                items.put(productId, cartItem);
            }
        }

        return Optional.of(new ShoppingCart(customerId, items));
    }

    @Override
    public boolean cartExists(String customerId) {
        String key = buildKey(customerId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void deleteCart(String customerId) {
        String key = buildKey(customerId);
        redisTemplate.delete(key);
    }

    private String buildKey(String customerId) {
        return RedisKeyConvention.buildKey(SERVICE, ENTITY, customerId);
    }
}
