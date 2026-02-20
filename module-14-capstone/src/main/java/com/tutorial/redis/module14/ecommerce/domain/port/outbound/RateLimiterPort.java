package com.tutorial.redis.module14.ecommerce.domain.port.outbound;

import com.tutorial.redis.module14.ecommerce.domain.model.RateLimitResult;

/**
 * Outbound port for rate limiting operations.
 *
 * <p>Abstracts a sliding window counter implemented as a Lua script
 * in Redis, returning whether the request is allowed and relevant
 * token/retry information.</p>
 */
public interface RateLimiterPort {

    RateLimitResult tryAcquire(String key, int maxTokens, int windowSeconds);
}
