package com.tutorial.redis.module02.domain.port.outbound;

import com.tutorial.redis.module02.domain.model.RankEntry;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for ranking / leaderboard operations.
 * Uses Redis Sorted Set structure (members with scores, automatically ranked).
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface TransactionRankingPort {

    void addOrUpdate(String rankingKey, String memberId, double score);

    double incrementScore(String rankingKey, String memberId, double delta);

    Optional<Double> getScore(String rankingKey, String memberId);

    Optional<Long> getRank(String rankingKey, String memberId);

    Optional<Long> getReverseRank(String rankingKey, String memberId);

    List<RankEntry> getTopN(String rankingKey, int n);

    long count(String rankingKey);

    long countByScoreRange(String rankingKey, double min, double max);

    void remove(String rankingKey, String memberId);
}
