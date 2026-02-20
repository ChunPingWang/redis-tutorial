package com.tutorial.redis.module14.ecommerce.application.usecase;

import com.tutorial.redis.module14.ecommerce.domain.model.RateLimitResult;
import com.tutorial.redis.module14.ecommerce.domain.port.inbound.RateLimitUseCase;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.RateLimiterPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service implementing rate limiting use cases.
 *
 * <p>Delegates to the {@link RateLimiterPort} for checking whether a
 * client request should be allowed based on a sliding window counter.</p>
 */
@Service
public class RateLimitService implements RateLimitUseCase {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "ecommerce:ratelimit:";

    private final RateLimiterPort rateLimiterPort;

    public RateLimitService(RateLimiterPort rateLimiterPort) {
        this.rateLimiterPort = rateLimiterPort;
    }

    @Override
    public RateLimitResult checkRateLimit(String clientId, int maxRequests, int windowSeconds) {
        log.info("Checking rate limit for client {} (max={}, window={}s)",
                clientId, maxRequests, windowSeconds);
        String key = RATE_LIMIT_KEY_PREFIX + clientId;
        return rateLimiterPort.tryAcquire(key, maxRequests, windowSeconds);
    }
}
