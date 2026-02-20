package com.tutorial.redis.module14.ecommerce.application.usecase;

import com.tutorial.redis.module14.ecommerce.domain.port.outbound.VisitorCountPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Utility application service for visitor analytics.
 *
 * <p>Delegates to the {@link VisitorCountPort} for recording page visits
 * and retrieving unique visitor counts using Redis HyperLogLog.</p>
 *
 * <p>This service has no inbound port interface as it is a utility
 * service used directly by other components.</p>
 */
@Service
public class VisitorAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(VisitorAnalyticsService.class);

    private final VisitorCountPort visitorCountPort;

    public VisitorAnalyticsService(VisitorCountPort visitorCountPort) {
        this.visitorCountPort = visitorCountPort;
    }

    public void recordPageVisit(String pageId, String visitorId) {
        log.info("Recording visit to page {} by visitor {}", pageId, visitorId);
        visitorCountPort.recordVisit(pageId, visitorId);
    }

    public long getUniqueVisitors(String pageId) {
        log.info("Getting unique visitor count for page {}", pageId);
        return visitorCountPort.getUniqueVisitorCount(pageId);
    }
}
