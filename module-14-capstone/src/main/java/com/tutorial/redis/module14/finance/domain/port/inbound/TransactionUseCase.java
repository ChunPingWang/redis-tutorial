package com.tutorial.redis.module14.finance.domain.port.inbound;

import com.tutorial.redis.module14.finance.domain.model.Transaction;

import java.util.List;

/**
 * Inbound port for transaction operations.
 *
 * <p>Defines use cases for recording financial transactions, retrieving
 * the top transactions by amount from a Redis sorted set leaderboard,
 * and searching indexed transactions via RediSearch.</p>
 */
public interface TransactionUseCase {

    /**
     * Records a transaction by adding it to both the leaderboard and
     * the search index.
     *
     * @param tx the transaction to record
     */
    void recordTransaction(Transaction tx);

    /**
     * Retrieves the top N transactions by amount from the leaderboard.
     *
     * @param count the number of top transactions to retrieve
     * @return list of transaction IDs ordered by amount (highest first)
     */
    List<String> getTopTransactions(int count);

    /**
     * Searches indexed transactions using a RediSearch query.
     *
     * @param query the RediSearch query string
     * @return list of matching results as JSON strings
     */
    List<String> searchTransactions(String query);
}
