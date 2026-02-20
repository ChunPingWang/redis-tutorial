package com.tutorial.redis.module10.adapter.outbound.redis;

import com.tutorial.redis.module10.domain.port.outbound.ClusterDataPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis adapter implementing {@link ClusterDataPort} using {@link StringRedisTemplate}.
 *
 * <p>This adapter provides data read/write operations suitable for both standalone
 * and cluster Redis deployments. In a real Redis Cluster environment, multi-key
 * operations require all keys to reside in the same hash slot. Callers should
 * use hash tags (e.g. {@code {user}:cart}, {@code {user}:orders}) to ensure
 * co-location.</p>
 *
 * <p>The {@link #writeMultipleKeys(Map)} method iterates and sets each key-value
 * pair individually, which works correctly in both standalone and cluster modes.
 * The {@link #readMultipleKeys(List)} method uses {@code multiGet} for efficient
 * batch retrieval.</p>
 */
@Component
public class RedisClusterDataAdapter implements ClusterDataPort {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisClusterDataAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeData(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readData(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates over each entry and writes it individually. In standalone mode
     * this is straightforward; in cluster mode each key is routed to the correct
     * node by the Lettuce cluster client. All keys should share the same hash tag
     * to guarantee they reside in the same slot.</p>
     */
    @Override
    public void writeMultipleKeys(Map<String, String> keyValues) {
        keyValues.forEach((key, value) ->
                stringRedisTemplate.opsForValue().set(key, value));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@code multiGet} for efficient batch retrieval. The returned map
     * preserves the insertion order of the input keys. Keys that do not exist
     * in Redis will have null values.</p>
     */
    @Override
    public Map<String, String> readMultipleKeys(List<String> keys) {
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            result.put(keys.get(i), values != null ? values.get(i) : null);
        }
        return result;
    }
}
