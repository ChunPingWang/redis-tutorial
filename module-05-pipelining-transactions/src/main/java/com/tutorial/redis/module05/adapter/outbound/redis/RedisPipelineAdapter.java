package com.tutorial.redis.module05.adapter.outbound.redis;

import com.tutorial.redis.module05.domain.port.outbound.PipelinePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis adapter implementing pipeline-based batch price operations.
 * Uses Redis pipelines to batch multiple GET/SET commands into a single
 * round-trip, significantly reducing network RTT for bulk operations.
 *
 * <p>Key format: {@code price:{productId}}</p>
 */
@Component
public class RedisPipelineAdapter implements PipelinePort {

    private static final Logger log = LoggerFactory.getLogger(RedisPipelineAdapter.class);
    private static final String KEY_PREFIX = "price:";

    private final StringRedisTemplate redisTemplate;

    public RedisPipelineAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Retrieves prices for multiple products using a single pipeline round-trip.
     * Each product ID is mapped to a Redis GET command; all commands are sent together.
     * Products not found in Redis will have a {@code null} value in the returned map.
     */
    @Override
    public Map<String, Double> batchGetPrices(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        log.debug("Pipeline batch GET for {} products", productIds.size());

        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String productId : productIds) {
                byte[] key = buildKey(productId).getBytes(StandardCharsets.UTF_8);
                connection.stringCommands().get(key);
            }
            // RedisCallback must return null when used with executePipelined
            return null;
        });

        Map<String, Double> priceMap = new LinkedHashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            String productId = productIds.get(i);
            Object result = results.get(i);
            if (result != null) {
                priceMap.put(productId, Double.parseDouble(result.toString()));
            } else {
                priceMap.put(productId, null);
            }
        }

        log.debug("Pipeline batch GET completed: {} results", priceMap.size());
        return priceMap;
    }

    /**
     * Sets prices for multiple products using a single pipeline round-trip.
     * Each entry in the map is converted to a Redis SET command; all commands
     * are sent together to minimize network overhead.
     */
    @Override
    public void batchSetPrices(Map<String, Double> prices) {
        if (prices == null || prices.isEmpty()) {
            return;
        }

        log.debug("Pipeline batch SET for {} products", prices.size());

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Map.Entry<String, Double> entry : prices.entrySet()) {
                byte[] key = buildKey(entry.getKey()).getBytes(StandardCharsets.UTF_8);
                byte[] value = String.valueOf(entry.getValue()).getBytes(StandardCharsets.UTF_8);
                connection.stringCommands().set(key, value);
            }
            // RedisCallback must return null when used with executePipelined
            return null;
        });

        log.debug("Pipeline batch SET completed for {} products", prices.size());
    }

    private String buildKey(String productId) {
        return KEY_PREFIX + productId;
    }
}
