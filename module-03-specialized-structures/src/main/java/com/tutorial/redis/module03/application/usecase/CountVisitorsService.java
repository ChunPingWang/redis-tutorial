package com.tutorial.redis.module03.application.usecase;

import com.tutorial.redis.module03.domain.port.inbound.CountVisitorsUseCase;
import com.tutorial.redis.module03.domain.port.outbound.UniqueVisitorPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service implementing unique visitor counting use cases.
 *
 * <p>Delegates to {@link UniqueVisitorPort} for Redis HyperLogLog operations.
 * Demonstrates PFADD, PFCOUNT, and PFMERGE for probabilistic cardinality estimation.</p>
 */
@Service
public class CountVisitorsService implements CountVisitorsUseCase {

    private final UniqueVisitorPort uniqueVisitorPort;

    public CountVisitorsService(UniqueVisitorPort uniqueVisitorPort) {
        this.uniqueVisitorPort = uniqueVisitorPort;
    }

    @Override
    public void recordVisit(String pageId, String date, String visitorId) {
        uniqueVisitorPort.addVisitor(pageId, date, visitorId);
    }

    @Override
    public long getDailyUniqueVisitors(String pageId, String date) {
        return uniqueVisitorPort.countVisitors(pageId, date);
    }

    @Override
    public long getWeeklyUniqueVisitors(String pageId, List<String> dailyKeys) {
        String destKey = "analytics:uv:" + pageId + ":weekly-merge";
        return uniqueVisitorPort.countMergedVisitors(destKey, dailyKeys);
    }
}
