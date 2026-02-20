package com.tutorial.redis.module03.domain.port.inbound;

import com.tutorial.redis.module03.domain.model.UserActivity;

/**
 * Inbound port: track user daily activity using Redis Bitmap structure.
 */
public interface TrackActivityUseCase {

    /**
     * Records that a user was active on a specific day.
     *
     * @param userId     the user identifier
     * @param yearMonth  the year-month string (format "YYYYMM")
     * @param dayOfMonth the day of the month (1-based)
     */
    void recordDailyActivity(String userId, String yearMonth, int dayOfMonth);

    /**
     * Checks whether a user was active on a specific day.
     *
     * @param userId     the user identifier
     * @param yearMonth  the year-month string (format "YYYYMM")
     * @param dayOfMonth the day of the month (1-based)
     * @return true if the user was active on that day
     */
    boolean wasActiveOnDay(String userId, String yearMonth, int dayOfMonth);

    /**
     * Retrieves the full monthly activity summary for a user.
     *
     * @param userId           the user identifier
     * @param yearMonth        the year-month string (format "YYYYMM")
     * @param totalDaysInMonth the total number of days in the month
     * @return the user's activity summary for the month
     */
    UserActivity getMonthlyActivity(String userId, String yearMonth, int totalDaysInMonth);
}
