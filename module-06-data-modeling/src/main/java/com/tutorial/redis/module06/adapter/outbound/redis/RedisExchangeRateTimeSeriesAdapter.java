package com.tutorial.redis.module06.adapter.outbound.redis;

import com.tutorial.redis.module06.domain.model.ExchangeRateSnapshot;
import com.tutorial.redis.module06.domain.port.outbound.ExchangeRateTimeSeriesPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Redis adapter implementing the exchange rate time-series using Sorted Sets.
 * Each currency pair has its own Sorted Set where the score is the epoch-millisecond
 * timestamp and the member encodes the rate and timestamp for uniqueness.
 *
 * <p>Member format: {@code "{rate}:{timestamp}"} (e.g., "31.5:1708000000000").
 * This format ensures uniqueness even if the same rate occurs at different timestamps.</p>
 *
 * <h3>Key Schema</h3>
 * <ul>
 *   <li>{@code rate:{currencyPair}} (ZSET, score=timestamp, member="{rate}:{timestamp}")</li>
 * </ul>
 */
@Component
public class RedisExchangeRateTimeSeriesAdapter implements ExchangeRateTimeSeriesPort {

    private static final Logger log = LoggerFactory.getLogger(RedisExchangeRateTimeSeriesAdapter.class);

    private static final String KEY_PREFIX = "rate:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisExchangeRateTimeSeriesAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Adds an exchange rate snapshot to the time series using {@code ZADD}.
     * The score is the snapshot's timestamp (epoch millis) and the member
     * is formatted as "{rate}:{timestamp}" for uniqueness.
     */
    @Override
    public void addSnapshot(ExchangeRateSnapshot snapshot) {
        String key = KEY_PREFIX + snapshot.getCurrencyPair();
        String member = snapshot.getRate() + ":" + snapshot.getTimestamp();

        stringRedisTemplate.opsForZSet().add(key, member, snapshot.getTimestamp());

        log.debug("Added rate snapshot for {} at {}: {} (member={})",
                snapshot.getCurrencyPair(), snapshot.getTimestamp(), snapshot.getRate(), member);
    }

    /**
     * Retrieves all snapshots for a currency pair within the given epoch-millisecond
     * range using {@code ZRANGEBYSCORE}. Each member is parsed back into an
     * {@link ExchangeRateSnapshot}.
     */
    @Override
    public List<ExchangeRateSnapshot> getSnapshots(String currencyPair, long fromEpoch, long toEpoch) {
        String key = KEY_PREFIX + currencyPair;

        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().rangeByScoreWithScores(key, fromEpoch, toEpoch);

        if (tuples == null || tuples.isEmpty()) {
            log.debug("No snapshots found for {} in range [{}, {}]", currencyPair, fromEpoch, toEpoch);
            return List.of();
        }

        List<ExchangeRateSnapshot> snapshots = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            ExchangeRateSnapshot snapshot = parseMember(currencyPair, tuple.getValue());
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }

        log.debug("Found {} snapshots for {} in range [{}, {}]",
                snapshots.size(), currencyPair, fromEpoch, toEpoch);
        return snapshots;
    }

    /**
     * Retrieves the most recent snapshot for a currency pair using
     * {@code ZREVRANGEBYSCORE} with a limit of 1 (reverse range from +inf
     * to -inf, taking only the first result).
     */
    @Override
    public Optional<ExchangeRateSnapshot> getLatestSnapshot(String currencyPair) {
        String key = KEY_PREFIX + currencyPair;

        Set<String> members = stringRedisTemplate.opsForZSet().reverseRangeByScore(
                key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0, 1
        );

        if (members == null || members.isEmpty()) {
            log.debug("No latest snapshot found for {}", currencyPair);
            return Optional.empty();
        }

        String member = members.iterator().next();
        ExchangeRateSnapshot snapshot = parseMember(currencyPair, member);

        if (snapshot != null) {
            log.debug("Latest snapshot for {}: rate={}, timestamp={}",
                    currencyPair, snapshot.getRate(), snapshot.getTimestamp());
        }

        return Optional.ofNullable(snapshot);
    }

    /**
     * Parses a Sorted Set member string in the format "{rate}:{timestamp}"
     * back into an {@link ExchangeRateSnapshot}.
     */
    private ExchangeRateSnapshot parseMember(String currencyPair, String member) {
        if (member == null) {
            return null;
        }

        // Member format: "rate:timestamp" — split from the last colon
        // since rate itself could be negative (e.g., "-0.5:1708000000000")
        int lastColon = member.lastIndexOf(':');
        if (lastColon <= 0 || lastColon >= member.length() - 1) {
            log.warn("Invalid member format: {}", member);
            return null;
        }

        try {
            double rate = Double.parseDouble(member.substring(0, lastColon));
            long timestamp = Long.parseLong(member.substring(lastColon + 1));
            return new ExchangeRateSnapshot(currencyPair, rate, timestamp);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse member '{}': {}", member, e.getMessage());
            return null;
        }
    }
}
