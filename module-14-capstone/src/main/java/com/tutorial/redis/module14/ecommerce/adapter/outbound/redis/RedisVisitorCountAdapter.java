package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.module14.ecommerce.domain.port.outbound.VisitorCountPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis adapter for visitor counting using HyperLogLog.
 *
 * <p>Implements {@link VisitorCountPort} using Redis HyperLogLog commands
 * for approximate unique visitor counting with minimal memory usage.</p>
 *
 * <p>Key format: {@code ecommerce:visitors:{pageId}}</p>
 */
@Component
public class RedisVisitorCountAdapter implements VisitorCountPort {

    private static final Logger log = LoggerFactory.getLogger(RedisVisitorCountAdapter.class);
    private static final String VISITOR_KEY_PREFIX = "ecommerce:visitors:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisVisitorCountAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void recordVisit(String pageId, String visitorId) {
        log.debug("Recording visit to page {} by visitor {}", pageId, visitorId);
        stringRedisTemplate.opsForHyperLogLog().add(VISITOR_KEY_PREFIX + pageId, visitorId);
    }

    @Override
    public long getUniqueVisitorCount(String pageId) {
        log.debug("Getting unique visitor count for page {}", pageId);
        return stringRedisTemplate.opsForHyperLogLog().size(VISITOR_KEY_PREFIX + pageId);
    }
}
