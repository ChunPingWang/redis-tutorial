package com.tutorial.redis.module12.adapter.outbound.redis;

import com.tutorial.redis.module12.domain.port.outbound.JsonDocumentPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis adapter for RedisJSON document operations using Lua scripts.
 *
 * <p>Invokes JSON.SET, JSON.GET, JSON.DEL, JSON.NUMINCRBY, and JSON.ARRAPPEND
 * via {@link DefaultRedisScript} because {@code connection.execute()} is not
 * supported for module commands in Lettuce / Spring Data Redis 4.x.</p>
 *
 * <p>Each method maps directly to a single RedisJSON command wrapped in a
 * minimal Lua script, keeping the adapter thin and the Redis interaction
 * transparent for educational purposes.</p>
 */
@Component
public class RedisJsonDocumentAdapter implements JsonDocumentPort {

    private static final Logger log = LoggerFactory.getLogger(RedisJsonDocumentAdapter.class);

    // JSON.SET KEYS[1] ARGV[1] ARGV[2]  -- key, path, jsonValue
    private static final DefaultRedisScript<String> JSON_SET = new DefaultRedisScript<>(
            "return redis.call('JSON.SET', KEYS[1], ARGV[1], ARGV[2])",
            String.class);

    // JSON.GET KEYS[1] ARGV[1]  -- key, path
    private static final DefaultRedisScript<String> JSON_GET = new DefaultRedisScript<>(
            "return redis.call('JSON.GET', KEYS[1], ARGV[1])",
            String.class);

    // JSON.DEL KEYS[1]  -- key (deletes entire document)
    private static final DefaultRedisScript<Long> JSON_DEL = new DefaultRedisScript<>(
            "return redis.call('JSON.DEL', KEYS[1])",
            Long.class);

    // JSON.NUMINCRBY KEYS[1] ARGV[1] ARGV[2]  -- key, path, increment
    private static final DefaultRedisScript<String> JSON_NUMINCRBY = new DefaultRedisScript<>(
            "return redis.call('JSON.NUMINCRBY', KEYS[1], ARGV[1], ARGV[2])",
            String.class);

    // JSON.ARRAPPEND KEYS[1] ARGV[1] ARGV[2]  -- key, path, jsonElement
    // tostring() converts the integer result to a string for consistent return type
    private static final DefaultRedisScript<String> JSON_ARRAPPEND = new DefaultRedisScript<>(
            "return tostring(redis.call('JSON.ARRAPPEND', KEYS[1], ARGV[1], ARGV[2]))",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisJsonDocumentAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void setDocument(String key, String path, String jsonValue) {
        stringRedisTemplate.execute(JSON_SET, List.of(key), path, jsonValue);
        log.debug("JSON.SET key='{}' path='{}'", key, path);
    }

    @Override
    public String getDocument(String key, String path) {
        String result = stringRedisTemplate.execute(JSON_GET, List.of(key), path);
        log.debug("JSON.GET key='{}' path='{}' -> {}", key, path,
                result != null ? result.substring(0, Math.min(result.length(), 80)) + "..." : "null");
        return result;
    }

    @Override
    public void deleteDocument(String key) {
        stringRedisTemplate.execute(JSON_DEL, List.of(key));
        log.debug("JSON.DEL key='{}'", key);
    }

    @Override
    public void incrementNumber(String key, String path, double value) {
        stringRedisTemplate.execute(JSON_NUMINCRBY, List.of(key), path, String.valueOf(value));
        log.debug("JSON.NUMINCRBY key='{}' path='{}' by {}", key, path, value);
    }

    @Override
    public void appendToArray(String key, String path, String jsonElement) {
        stringRedisTemplate.execute(JSON_ARRAPPEND, List.of(key), path, jsonElement);
        log.debug("JSON.ARRAPPEND key='{}' path='{}'", key, path);
    }
}
