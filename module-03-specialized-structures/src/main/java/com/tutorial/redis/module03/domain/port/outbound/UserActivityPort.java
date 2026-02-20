package com.tutorial.redis.module03.domain.port.outbound;

/**
 * Outbound port for user activity tracking operations.
 * Uses Redis Bitmap structure (SETBIT / GETBIT / BITCOUNT).
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface UserActivityPort {

    /**
     * Records that a user was active on a given day (SETBIT).
     *
     * @param userId     the user identifier
     * @param yearMonth  the year-month string (format "YYYYMM")
     * @param dayOfMonth the day of the month (1-based)
     */
    void recordActivity(String userId, String yearMonth, int dayOfMonth);

    /**
     * Checks whether a user was active on a given day (GETBIT).
     *
     * @param userId     the user identifier
     * @param yearMonth  the year-month string (format "YYYYMM")
     * @param dayOfMonth the day of the month (1-based)
     * @return true if the user was active on that day
     */
    boolean isActive(String userId, String yearMonth, int dayOfMonth);

    /**
     * Counts the total number of active days in a month (BITCOUNT).
     *
     * @param userId    the user identifier
     * @param yearMonth the year-month string (format "YYYYMM")
     * @return the number of days the user was active
     */
    long countActiveDays(String userId, String yearMonth);

    /**
     * Counts the number of active days within a day range in a month (BITCOUNT with range).
     *
     * @param userId    the user identifier
     * @param yearMonth the year-month string (format "YYYYMM")
     * @param fromDay   start day (1-based, inclusive)
     * @param toDay     end day (1-based, inclusive)
     * @return the number of active days within the range
     */
    long countActiveDaysInRange(String userId, String yearMonth, int fromDay, int toDay);
}
