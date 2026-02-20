package com.tutorial.redis.module02.adapter.outbound.redis;

import com.tutorial.redis.module02.domain.model.RankEntry;
import com.tutorial.redis.module02.domain.port.outbound.TransactionRankingPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Redis adapter for ranking/leaderboard operations using ZSetOperations (Redis Sorted Set).
 *
 * <p>Uses {@link StringRedisTemplate} because members are string IDs
 * and scores are doubles — no JSON serialization needed.</p>
 *
 * <p>Key pattern: {@code ranking:{rankingKey}}<br>
 * Supports Redis sorted set operations: ZADD, ZINCRBY, ZSCORE, ZRANK,
 * ZREVRANK, ZREVRANGEBYSCORE, ZCARD, ZCOUNT, ZREM.</p>
 */
@Component
public class RedisTransactionRankingAdapter implements TransactionRankingPort {

    private static final String KEY_PREFIX = "ranking:";

    private final StringRedisTemplate redisTemplate;

    public RedisTransactionRankingAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addOrUpdate(String rankingKey, String memberId, double score) {
        String fullKey = buildKey(rankingKey);
        redisTemplate.opsForZSet().add(fullKey, memberId, score);
    }

    @Override
    public double incrementScore(String rankingKey, String memberId, double delta) {
        String fullKey = buildKey(rankingKey);
        Double result = redisTemplate.opsForZSet().incrementScore(fullKey, memberId, delta);
        return result != null ? result : 0.0;
    }

    @Override
    public Optional<Double> getScore(String rankingKey, String memberId) {
        String fullKey = buildKey(rankingKey);
        Double score = redisTemplate.opsForZSet().score(fullKey, memberId);
        return Optional.ofNullable(score);
    }

    @Override
    public Optional<Long> getRank(String rankingKey, String memberId) {
        String fullKey = buildKey(rankingKey);
        Long rank = redisTemplate.opsForZSet().rank(fullKey, memberId);
        return Optional.ofNullable(rank);
    }

    @Override
    public Optional<Long> getReverseRank(String rankingKey, String memberId) {
        String fullKey = buildKey(rankingKey);
        Long rank = redisTemplate.opsForZSet().reverseRank(fullKey, memberId);
        return Optional.ofNullable(rank);
    }

    @Override
    public List<RankEntry> getTopN(String rankingKey, int n) {
        String fullKey = buildKey(rankingKey);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(fullKey, 0, n - 1L);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<RankEntry> entries = new ArrayList<>();
        long rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String memberId = tuple.getValue();
            Double score = tuple.getScore();
            if (memberId != null && score != null) {
                entries.add(new RankEntry(memberId, score, rank));
                rank++;
            }
        }
        return entries;
    }

    @Override
    public long count(String rankingKey) {
        String fullKey = buildKey(rankingKey);
        Long size = redisTemplate.opsForZSet().zCard(fullKey);
        return size != null ? size : 0L;
    }

    @Override
    public long countByScoreRange(String rankingKey, double min, double max) {
        String fullKey = buildKey(rankingKey);
        Long count = redisTemplate.opsForZSet().count(fullKey, min, max);
        return count != null ? count : 0L;
    }

    @Override
    public void remove(String rankingKey, String memberId) {
        String fullKey = buildKey(rankingKey);
        redisTemplate.opsForZSet().remove(fullKey, memberId);
    }

    private String buildKey(String rankingKey) {
        return KEY_PREFIX + rankingKey;
    }
}
