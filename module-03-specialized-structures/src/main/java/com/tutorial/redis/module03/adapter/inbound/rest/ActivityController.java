package com.tutorial.redis.module03.adapter.inbound.rest;

import com.tutorial.redis.module03.application.dto.UserActivityResponse;
import com.tutorial.redis.module03.domain.model.UserActivity;
import com.tutorial.redis.module03.domain.port.inbound.TrackActivityUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for user activity tracking.
 *
 * <p>Demonstrates Redis Bitmap operations (SETBIT, GETBIT, BITCOUNT)
 * through daily activity recording and monthly activity summary endpoints.</p>
 */
@RestController
@RequestMapping("/api/v1/activity")
public class ActivityController {

    private final TrackActivityUseCase trackActivityUseCase;

    public ActivityController(TrackActivityUseCase trackActivityUseCase) {
        this.trackActivityUseCase = trackActivityUseCase;
    }

    @PostMapping("/{userId}/{yearMonth}/{day}")
    public ResponseEntity<Map<String, Object>> recordDailyActivity(
            @PathVariable String userId,
            @PathVariable String yearMonth,
            @PathVariable int day) {
        trackActivityUseCase.recordDailyActivity(userId, yearMonth, day);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "yearMonth", yearMonth,
                "day", day,
                "recorded", true));
    }

    @GetMapping("/{userId}/{yearMonth}/{day}")
    public ResponseEntity<Map<String, Object>> wasActiveOnDay(
            @PathVariable String userId,
            @PathVariable String yearMonth,
            @PathVariable int day) {
        boolean active = trackActivityUseCase.wasActiveOnDay(userId, yearMonth, day);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "yearMonth", yearMonth,
                "day", day,
                "active", active));
    }

    @GetMapping("/{userId}/{yearMonth}/summary")
    public ResponseEntity<UserActivityResponse> getMonthlyActivity(
            @PathVariable String userId,
            @PathVariable String yearMonth,
            @RequestParam int totalDays) {
        UserActivity activity = trackActivityUseCase.getMonthlyActivity(userId, yearMonth, totalDays);
        return ResponseEntity.ok(UserActivityResponse.from(activity));
    }
}
