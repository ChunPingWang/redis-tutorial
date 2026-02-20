package com.tutorial.redis.module03.domain.model;

import java.util.Objects;

/**
 * Represents a unique visitor count for a page over a given period.
 * Maps to Redis HyperLogLog structure (PFADD / PFCOUNT / PFMERGE).
 * Immutable value object — all fields are final.
 */
public class UniqueVisitorCount {

    private final String pageId;
    private final String period;
    private final long estimatedCount;

    public UniqueVisitorCount(String pageId, String period, long estimatedCount) {
        this.pageId = Objects.requireNonNull(pageId, "pageId must not be null");
        this.period = Objects.requireNonNull(period, "period must not be null");
        this.estimatedCount = estimatedCount;
    }

    public String getPageId() { return pageId; }
    public String getPeriod() { return period; }
    public long getEstimatedCount() { return estimatedCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UniqueVisitorCount that)) return false;
        return pageId.equals(that.pageId) && period.equals(that.period);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, period);
    }

    @Override
    public String toString() {
        return "UniqueVisitorCount{pageId='%s', period='%s', estimatedCount=%d}".formatted(
                pageId, period, estimatedCount);
    }
}
