package com.tutorial.redis.module03.domain.port.inbound;

import java.util.List;

/**
 * Inbound port: count unique visitors using Redis HyperLogLog structure.
 */
public interface CountVisitorsUseCase {

    /**
     * Records a visit to a page on a given date.
     *
     * @param pageId    the page identifier
     * @param date      the date string (e.g., "2026-02-20")
     * @param visitorId the visitor identifier
     */
    void recordVisit(String pageId, String date, String visitorId);

    /**
     * Gets the estimated number of unique visitors for a page on a specific date.
     *
     * @param pageId the page identifier
     * @param date   the date string (e.g., "2026-02-20")
     * @return the estimated unique visitor count
     */
    long getDailyUniqueVisitors(String pageId, String date);

    /**
     * Gets the estimated number of unique visitors across multiple daily keys (e.g., a week).
     * Uses PFMERGE to combine daily HyperLogLogs.
     *
     * @param pageId    the page identifier
     * @param dailyKeys the list of daily keys to merge
     * @return the estimated unique visitor count across the merged period
     */
    long getWeeklyUniqueVisitors(String pageId, List<String> dailyKeys);
}
