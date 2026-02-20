package com.tutorial.redis.module03.application.usecase;

import com.tutorial.redis.module03.domain.model.UserActivity;
import com.tutorial.redis.module03.domain.port.inbound.TrackActivityUseCase;
import com.tutorial.redis.module03.domain.port.outbound.UserActivityPort;
import org.springframework.stereotype.Service;

/**
 * Application service implementing user activity tracking use cases.
 *
 * <p>Delegates to {@link UserActivityPort} for Redis Bitmap operations.
 * Demonstrates SETBIT, GETBIT, and BITCOUNT for daily activity tracking.</p>
 */
@Service
public class TrackActivityService implements TrackActivityUseCase {

    private final UserActivityPort userActivityPort;

    public TrackActivityService(UserActivityPort userActivityPort) {
        this.userActivityPort = userActivityPort;
    }

    @Override
    public void recordDailyActivity(String userId, String yearMonth, int dayOfMonth) {
        userActivityPort.recordActivity(userId, yearMonth, dayOfMonth);
    }

    @Override
    public boolean wasActiveOnDay(String userId, String yearMonth, int dayOfMonth) {
        return userActivityPort.isActive(userId, yearMonth, dayOfMonth);
    }

    @Override
    public UserActivity getMonthlyActivity(String userId, String yearMonth, int totalDaysInMonth) {
        long activeDays = userActivityPort.countActiveDays(userId, yearMonth);
        return new UserActivity(userId, yearMonth, (int) activeDays, totalDaysInMonth);
    }
}
