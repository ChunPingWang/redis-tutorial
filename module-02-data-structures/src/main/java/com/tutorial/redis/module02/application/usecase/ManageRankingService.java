package com.tutorial.redis.module02.application.usecase;

import com.tutorial.redis.module02.domain.model.RankEntry;
import com.tutorial.redis.module02.domain.port.inbound.ManageRankingUseCase;
import com.tutorial.redis.module02.domain.port.outbound.TransactionRankingPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementing ranking/leaderboard management use cases.
 *
 * <p>Delegates to {@link TransactionRankingPort} for Redis Sorted Set operations.
 * Demonstrates ZADD, ZINCRBY, ZREVRANGEBYSCORE for leaderboard management.</p>
 */
@Service
public class ManageRankingService implements ManageRankingUseCase {

    private final TransactionRankingPort transactionRankingPort;

    public ManageRankingService(TransactionRankingPort transactionRankingPort) {
        this.transactionRankingPort = transactionRankingPort;
    }

    @Override
    public void submitScore(String rankingKey, String memberId, double score) {
        transactionRankingPort.addOrUpdate(rankingKey, memberId, score);
    }

    @Override
    public double addToScore(String rankingKey, String memberId, double delta) {
        return transactionRankingPort.incrementScore(rankingKey, memberId, delta);
    }

    @Override
    public List<RankEntry> getLeaderboard(String rankingKey, int topN) {
        return transactionRankingPort.getTopN(rankingKey, topN);
    }

    @Override
    public Optional<RankEntry> getMemberRank(String rankingKey, String memberId) {
        Optional<Long> reverseRank = transactionRankingPort.getReverseRank(rankingKey, memberId);
        Optional<Double> score = transactionRankingPort.getScore(rankingKey, memberId);

        if (reverseRank.isPresent() && score.isPresent()) {
            // reverseRank is 0-based, display rank is 1-based
            long displayRank = reverseRank.get() + 1;
            return Optional.of(new RankEntry(memberId, score.get(), displayRank));
        }
        return Optional.empty();
    }
}
