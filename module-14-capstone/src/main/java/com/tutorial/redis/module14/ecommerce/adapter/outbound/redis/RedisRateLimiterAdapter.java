package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.module14.ecommerce.domain.model.RateLimitResult;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.RateLimiterPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis adapter for rate limiting using a sliding window counter.
 *
 * <p>Implements {@link RateLimiterPort} using a Lua script that atomically
 * checks and increments a counter with TTL-based expiration. The counter
 * tracks the number of requests in the current window.</p>
 *
 * <p>Lua script behavior:</p>
 * <ul>
 *   <li>If the current count is below the maximum, increment and return
 *       the remaining tokens as a non-negative number string</li>
 *   <li>If the limit is exceeded, return a negative number representing
 *       the TTL in seconds until the window resets</li>
 * </ul>
 */
@Component
public class RedisRateLimiterAdapter implements RateLimiterPort {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterAdapter.class);

    private static final DefaultRedisScript<String> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local key = KEYS[1]\n" +
            "local max = tonumber(ARGV[1])\n" +
            "local window = tonumber(ARGV[2])\n" +
            "local current = tonumber(redis.call('GET', key) or '0')\n" +
            "if current < max then\n" +
            "    redis.call('INCR', key)\n" +
            "    if current == 0 then redis.call('EXPIRE', key, window) end\n" +
            "    return tostring(max - current - 1)\n" +
            "else\n" +
            "    local ttl = redis.call('TTL', key)\n" +
            "    return '-' .. tostring(ttl)\n" +
            "end",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisRateLimiterAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public RateLimitResult tryAcquire(String key, int maxTokens, int windowSeconds) {
        log.debug("Trying to acquire rate limit token for key {} (max={}, window={}s)",
                key, maxTokens, windowSeconds);

        String result = stringRedisTemplate.execute(RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(maxTokens),
                String.valueOf(windowSeconds));

        if (result == null) {
            log.warn("Rate limit script returned null for key {}", key);
            return new RateLimitResult(false, 0, 0);
        }

        if (result.startsWith("-")) {
            // Request denied — parse TTL from the result
            long ttlSeconds = Long.parseLong(result.substring(1));
            long retryAfterMs = ttlSeconds * 1000;
            log.debug("Rate limit exceeded for key {}, retry after {} ms", key, retryAfterMs);
            return new RateLimitResult(false, 0, retryAfterMs);
        } else {
            // Request allowed — parse remaining tokens
            int remaining = Integer.parseInt(result);
            log.debug("Rate limit allowed for key {}, remaining tokens: {}", key, remaining);
            return new RateLimitResult(true, remaining, 0);
        }
    }
}
