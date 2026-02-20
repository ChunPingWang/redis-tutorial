package com.tutorial.redis.module02.domain.port.inbound;

import com.tutorial.redis.module02.domain.model.RankEntry;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: manage rankings / leaderboards using Redis Sorted Set structure.
 */
public interface ManageRankingUseCase {

    void submitScore(String rankingKey, String memberId, double score);

    double addToScore(String rankingKey, String memberId, double delta);

    List<RankEntry> getLeaderboard(String rankingKey, int topN);

    Optional<RankEntry> getMemberRank(String rankingKey, String memberId);
}
