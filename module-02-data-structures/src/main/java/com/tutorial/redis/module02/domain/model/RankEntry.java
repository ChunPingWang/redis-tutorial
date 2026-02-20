package com.tutorial.redis.module02.domain.model;

import java.util.Objects;

/**
 * Represents an entry in a ranking / leaderboard.
 * Maps to Redis Sorted Set structure (member with score and rank).
 * Immutable value object — all fields are final.
 */
public class RankEntry {

    private final String memberId;
    private final double score;
    private final long rank;

    public RankEntry(String memberId, double score, long rank) {
        this.memberId = Objects.requireNonNull(memberId, "memberId must not be null");
        this.score = score;
        this.rank = rank;
    }

    public String getMemberId() { return memberId; }
    public double getScore() { return score; }
    public long getRank() { return rank; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RankEntry that)) return false;
        return memberId.equals(that.memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId);
    }

    @Override
    public String toString() {
        return "RankEntry{memberId='%s', score=%.2f, rank=%d}".formatted(memberId, score, rank);
    }
}
