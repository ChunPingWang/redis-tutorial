package com.tutorial.redis.module14.ecommerce.domain.port.inbound;

import com.tutorial.redis.module14.ecommerce.domain.model.RateLimitResult;

/**
 * Inbound port for rate limiting operations.
 *
 * <p>Defines the use case for checking whether a client request should
 * be allowed based on a sliding window rate limiter backed by Redis.</p>
 */
public interface RateLimitUseCase {

    RateLimitResult checkRateLimit(String clientId, int maxRequests, int windowSeconds);
}
