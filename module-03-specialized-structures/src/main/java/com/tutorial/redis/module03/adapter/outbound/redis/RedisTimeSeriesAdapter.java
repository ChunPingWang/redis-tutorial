package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.module03.domain.model.TimeSeriesDataPoint;
import com.tutorial.redis.module03.domain.port.outbound.TimeSeriesPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Redis adapter for time series operations using Redis TimeSeries module commands.
 *
 * <p>Uses Lua scripts to invoke TimeSeries module commands
 * (TS.CREATE, TS.ADD, TS.RANGE, TS.GET).</p>
 *
 * <p>Key pattern: {@code ts:{key}}</p>
 */
@Component
public class RedisTimeSeriesAdapter implements TimeSeriesPort {

    private static final String KEY_PREFIX = "ts";

    private static final DefaultRedisScript<Long> TS_CREATE_SIMPLE =
            new DefaultRedisScript<>("redis.call('TS.CREATE', KEYS[1], 'RETENTION', ARGV[1]); return 1", Long.class);

    private static final DefaultRedisScript<Long> TS_ADD =
            new DefaultRedisScript<>("return redis.call('TS.ADD', KEYS[1], ARGV[1], ARGV[2])", Long.class);

    private static final DefaultRedisScript<Long> TS_ADD_AUTO =
            new DefaultRedisScript<>("return redis.call('TS.ADD', KEYS[1], '*', ARGV[1])", Long.class);

    private static final DefaultRedisScript<List> TS_RANGE =
            new DefaultRedisScript<>("return redis.call('TS.RANGE', KEYS[1], ARGV[1], ARGV[2])", List.class);

    private static final DefaultRedisScript<List> TS_GET =
            new DefaultRedisScript<>("return redis.call('TS.GET', KEYS[1])", List.class);

    private final StringRedisTemplate redisTemplate;

    public RedisTimeSeriesAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void create(String key, long retentionMs, Map<String, String> labels) {
        String tsKey = buildKey(key);
        if (labels == null || labels.isEmpty()) {
            redisTemplate.execute(TS_CREATE_SIMPLE, List.of(tsKey), String.valueOf(retentionMs));
        } else {
            StringBuilder lua = new StringBuilder("redis.call('TS.CREATE', KEYS[1], 'RETENTION', ARGV[1], 'LABELS'");
            int argIdx = 2;
            for (Map.Entry<String, String> ignored : labels.entrySet()) {
                lua.append(", ARGV[").append(argIdx++).append("]");
                lua.append(", ARGV[").append(argIdx++).append("]");
            }
            lua.append("); return 1");

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(lua.toString(), Long.class);
            List<String> args = new ArrayList<>();
            args.add(String.valueOf(retentionMs));
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                args.add(entry.getKey());
                args.add(entry.getValue());
            }
            redisTemplate.execute(script, List.of(tsKey), args.toArray(new String[0]));
        }
    }

    @Override
    public void add(String key, long timestamp, double value) {
        String tsKey = buildKey(key);
        redisTemplate.execute(TS_ADD, List.of(tsKey),
                String.valueOf(timestamp), String.valueOf(value));
    }

    @Override
    public void addAutoTimestamp(String key, double value) {
        String tsKey = buildKey(key);
        redisTemplate.execute(TS_ADD_AUTO, List.of(tsKey), String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<TimeSeriesDataPoint> range(String key, long fromTimestamp, long toTimestamp) {
        String tsKey = buildKey(key);
        List<Object> result = redisTemplate.execute(TS_RANGE, List.of(tsKey),
                String.valueOf(fromTimestamp), String.valueOf(toTimestamp));

        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }

        List<TimeSeriesDataPoint> points = new ArrayList<>(result.size());
        for (Object entry : result) {
            if (entry instanceof List<?> pair && pair.size() >= 2) {
                long ts = parseLong(pair.get(0));
                double val = parseDouble(pair.get(1));
                points.add(new TimeSeriesDataPoint(ts, val));
            }
        }
        return points;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<TimeSeriesDataPoint> getLatest(String key) {
        String tsKey = buildKey(key);
        List<Object> result = redisTemplate.execute(TS_GET, List.of(tsKey));

        if (result == null || result.size() < 2) {
            return Optional.empty();
        }

        long ts = parseLong(result.get(0));
        double val = parseDouble(result.get(1));
        return Optional.of(new TimeSeriesDataPoint(ts, val));
    }

    private long parseLong(Object raw) {
        if (raw instanceof Long l) return l;
        if (raw instanceof Number n) return n.longValue();
        return Long.parseLong(raw.toString());
    }

    private double parseDouble(Object raw) {
        if (raw instanceof Number n) return n.doubleValue();
        return Double.parseDouble(raw.toString());
    }

    private String buildKey(String key) {
        return KEY_PREFIX + ":" + key;
    }
}
