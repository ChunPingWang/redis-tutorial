package com.tutorial.redis.module03.domain.model;

import java.util.Objects;

/**
 * Represents a user's activity summary for a given month.
 * Maps to Redis Bitmap structure (SETBIT / GETBIT / BITCOUNT).
 * Immutable value object — all fields are final.
 */
public class UserActivity {

    private final String userId;
    private final String yearMonth;
    private final int activeDays;
    private final int totalDays;

    public UserActivity(String userId, String yearMonth, int activeDays, int totalDays) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.yearMonth = Objects.requireNonNull(yearMonth, "yearMonth must not be null");
        if (activeDays < 0) {
            throw new IllegalArgumentException("activeDays must not be negative, got: " + activeDays);
        }
        if (totalDays <= 0) {
            throw new IllegalArgumentException("totalDays must be greater than 0, got: " + totalDays);
        }
        if (activeDays > totalDays) {
            throw new IllegalArgumentException(
                    "activeDays (%d) must not exceed totalDays (%d)".formatted(activeDays, totalDays));
        }
        this.activeDays = activeDays;
        this.totalDays = totalDays;
    }

    public String getUserId() { return userId; }
    public String getYearMonth() { return yearMonth; }
    public int getActiveDays() { return activeDays; }
    public int getTotalDays() { return totalDays; }

    /**
     * Calculates the activity rate as a ratio of active days to total days.
     *
     * @return activity rate between 0.0 and 1.0
     */
    public double activityRate() {
        return activeDays / (double) totalDays;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserActivity that)) return false;
        return userId.equals(that.userId) && yearMonth.equals(that.yearMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, yearMonth);
    }

    @Override
    public String toString() {
        return "UserActivity{userId='%s', yearMonth='%s', activeDays=%d, totalDays=%d, rate=%.2f}".formatted(
                userId, yearMonth, activeDays, totalDays, activityRate());
    }
}
