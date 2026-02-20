package com.tutorial.redis.module14.ecommerce.domain.port.outbound;

/**
 * Outbound port for visitor counting operations.
 *
 * <p>Abstracts Redis HyperLogLog commands for recording page visits
 * and retrieving approximate unique visitor counts.</p>
 */
public interface VisitorCountPort {

    void recordVisit(String pageId, String visitorId);

    long getUniqueVisitorCount(String pageId);
}
