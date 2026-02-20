package com.tutorial.redis.module02.application.dto;

import com.tutorial.redis.module02.domain.model.RankEntry;

/**
 * Response DTO for ranking/leaderboard entries.
 */
public record RankEntryResponse(
        long rank,
        String memberId,
        double score
) {
    public static RankEntryResponse from(RankEntry entry) {
        return new RankEntryResponse(
                entry.getRank(),
                entry.getMemberId(),
                entry.getScore()
        );
    }
}
