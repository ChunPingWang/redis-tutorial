package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.module14.ecommerce.domain.port.outbound.CartPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis adapter for shopping cart persistence using Hash operations.
 *
 * <p>Implements {@link CartPort} by storing each cart item as a field
 * in a Redis Hash keyed by the customer ID. The field key is the product
 * ID and the value is the serialized cart item string.</p>
 *
 * <p>Cart key format: {@code ecommerce:cart:{customerId}}</p>
 */
@Component
public class RedisCartAdapter implements CartPort {

    private static final Logger log = LoggerFactory.getLogger(RedisCartAdapter.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisCartAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void addItem(String cartKey, String productId, String itemJson) {
        log.debug("Adding item {} to cart {}", productId, cartKey);
        stringRedisTemplate.opsForHash().put(cartKey, productId, itemJson);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getAllItems(String cartKey) {
        log.debug("Retrieving all items from cart {}", cartKey);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(cartKey);
        if (entries.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            result.put((String) entry.getKey(), (String) entry.getValue());
        }
        return result;
    }

    @Override
    public void deleteCart(String cartKey) {
        log.debug("Deleting cart {}", cartKey);
        stringRedisTemplate.delete(cartKey);
    }
}
