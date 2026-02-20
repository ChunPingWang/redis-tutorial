package com.tutorial.redis.module14.finance.domain.port.outbound;

import java.util.List;

/**
 * Outbound port for transaction ranking operations.
 *
 * <p>Abstracts a Redis sorted set leaderboard that ranks transactions
 * by amount. Used to quickly retrieve the highest-value transactions.</p>
 */
public interface TransactionRankingPort {

    /**
     * Adds a transaction to the leaderboard sorted set.
     *
     * @param txId   the transaction identifier (member)
     * @param amount the transaction amount (score)
     */
    void addToLeaderboard(String txId, double amount);

    /**
     * Retrieves the top N transactions by amount (highest first).
     *
     * @param n the number of top entries to retrieve
     * @return list of transaction IDs ordered by descending amount
     */
    List<String> getTopN(int n);
}
