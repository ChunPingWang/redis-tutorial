package com.tutorial.redis.module03.application.dto;

import com.tutorial.redis.module03.domain.model.UserActivity;

/**
 * Response DTO for user activity summary.
 */
public record UserActivityResponse(
        String userId,
        String yearMonth,
        int activeDays,
        int totalDays,
        double activityRate
) {
    public static UserActivityResponse from(UserActivity activity) {
        return new UserActivityResponse(
                activity.getUserId(),
                activity.getYearMonth(),
                activity.getActiveDays(),
                activity.getTotalDays(),
                activity.activityRate()
        );
    }
}
