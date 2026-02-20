package com.tutorial.redis.module03.domain.port.outbound;

import java.util.List;

/**
 * Outbound port for unique visitor counting operations.
 * Uses Redis HyperLogLog structure (PFADD / PFCOUNT / PFMERGE).
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface UniqueVisitorPort {

    /**
     * Adds a visitor to the HyperLogLog for a page and period (PFADD).
     *
     * @param pageId    the page identifier
     * @param period    the time period (e.g., "2026-02-20", "2026-W08", "2026-02")
     * @param visitorId the visitor identifier
     * @return true if the internal HyperLogLog was modified (i.e., likely a new visitor)
     */
    boolean addVisitor(String pageId, String period, String visitorId);

    /**
     * Counts the estimated number of unique visitors for a page and period (PFCOUNT).
     *
     * @param pageId the page identifier
     * @param period the time period
     * @return the estimated unique visitor count
     */
    long countVisitors(String pageId, String period);

    /**
     * Merges multiple HyperLogLog source keys into a destination and returns the count (PFMERGE + PFCOUNT).
     *
     * @param destKey    the destination key for the merged HyperLogLog
     * @param sourceKeys the list of source keys to merge
     * @return the estimated unique visitor count of the merged set
     */
    long countMergedVisitors(String destKey, List<String> sourceKeys);
}
