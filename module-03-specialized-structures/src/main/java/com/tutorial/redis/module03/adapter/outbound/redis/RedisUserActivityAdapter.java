package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.common.config.RedisKeyConvention;
import com.tutorial.redis.module03.domain.port.outbound.UserActivityPort;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis adapter for user activity tracking using Bitmap operations.
 *
 * <p>Uses {@link StringRedisTemplate} with {@code opsForValue().setBit()} / {@code getBit()}
 * for per-day activity tracking, and {@code RedisCallback} for BITCOUNT operations.</p>
 *
 * <p>Key pattern: {@code banking:activity:{userId}:{yearMonth}}</p>
 *
 * <p>Each bit in the bitmap represents a day of the month.
 * Bit offset 0 = day 1, bit offset 1 = day 2, etc.</p>
 */
@Component
public class RedisUserActivityAdapter implements UserActivityPort {

    private static final String SERVICE = "banking";
    private static final String ENTITY = "activity";

    private final StringRedisTemplate redisTemplate;

    public RedisUserActivityAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void recordActivity(String userId, String yearMonth, int dayOfMonth) {
        String key = buildKey(userId, yearMonth);
        redisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
    }

    @Override
    public boolean isActive(String userId, String yearMonth, int dayOfMonth) {
        String key = buildKey(userId, yearMonth);
        Boolean result = redisTemplate.opsForValue().getBit(key, dayOfMonth - 1);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public long countActiveDays(String userId, String yearMonth) {
        String key = buildKey(userId, yearMonth);
        Long count = redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.stringCommands().bitCount(key.getBytes()));
        return count != null ? count : 0L;
    }

    @Override
    public long countActiveDaysInRange(String userId, String yearMonth, int fromDay, int toDay) {
        String key = buildKey(userId, yearMonth);
        // NOTE: Redis BITCOUNT range operates on byte boundaries, not bit boundaries.
        // For bit-level precision, we iterate with GETBIT for each day in the range.
        // This is acceptable for monthly bitmaps (max 31 days).
        long count = 0;
        for (int day = fromDay; day <= toDay; day++) {
            Boolean active = redisTemplate.opsForValue().getBit(key, day - 1);
            if (Boolean.TRUE.equals(active)) {
                count++;
            }
        }
        return count;
    }

    private String buildKey(String userId, String yearMonth) {
        return RedisKeyConvention.buildKey(SERVICE, ENTITY, userId) + ":" + yearMonth;
    }
}
