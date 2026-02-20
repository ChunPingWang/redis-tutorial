package com.tutorial.redis.module11.adapter.outbound.redis;

import com.tutorial.redis.module11.domain.model.ProductIndex;
import com.tutorial.redis.module11.domain.port.outbound.ProductDataPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis adapter for persisting product data as Redis Hashes.
 *
 * <p>Products are stored as Hashes with key pattern {@code product:{productId}}
 * so that RediSearch can index them via a prefix-based FT.CREATE.</p>
 *
 * <p>Uses {@link StringRedisTemplate} and {@code opsForHash().putAll()} for
 * straightforward Hash operations — no Lua scripts needed here.</p>
 */
@Component
public class RedisProductDataAdapter implements ProductDataPort {

    private static final Logger log = LoggerFactory.getLogger(RedisProductDataAdapter.class);

    private static final String KEY_PREFIX = "product:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisProductDataAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void saveProduct(ProductIndex product) {
        String key = KEY_PREFIX + product.getProductId();
        Map<String, String> fields = toHashFields(product);
        stringRedisTemplate.opsForHash().putAll(key, fields);
        log.debug("Saved product Hash '{}'", key);
    }

    @Override
    public void saveProducts(List<ProductIndex> products) {
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (ProductIndex product : products) {
                String key = KEY_PREFIX + product.getProductId();
                Map<String, String> fields = toHashFields(product);
                stringRedisTemplate.opsForHash().putAll(key, fields);
            }
            return null;
        });
        log.info("Saved {} products as Redis Hashes via pipeline", products.size());
    }

    /**
     * Converts a {@link ProductIndex} to a flat map of string field names to string values
     * suitable for storage as a Redis Hash.
     *
     * @param product the product to convert
     * @return map of field name to string value
     */
    private Map<String, String> toHashFields(ProductIndex product) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name", product.getName());
        fields.put("description", product.getDescription());
        fields.put("category", product.getCategory());
        fields.put("price", String.valueOf(product.getPrice()));
        fields.put("brand", product.getBrand());
        return fields;
    }
}
