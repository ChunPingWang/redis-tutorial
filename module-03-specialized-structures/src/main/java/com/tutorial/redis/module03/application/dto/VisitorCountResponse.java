package com.tutorial.redis.module03.application.dto;

import com.tutorial.redis.module03.domain.model.UniqueVisitorCount;

/**
 * Response DTO for unique visitor count.
 */
public record VisitorCountResponse(
        String pageId,
        String period,
        long estimatedCount
) {
    public static VisitorCountResponse from(UniqueVisitorCount count) {
        return new VisitorCountResponse(
                count.getPageId(),
                count.getPeriod(),
                count.getEstimatedCount()
        );
    }
}
