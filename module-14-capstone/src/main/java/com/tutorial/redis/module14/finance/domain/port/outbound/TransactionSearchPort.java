package com.tutorial.redis.module14.finance.domain.port.outbound;

import com.tutorial.redis.module14.finance.domain.model.Transaction;

import java.util.List;

/**
 * Outbound port for transaction search operations.
 *
 * <p>Abstracts the RediSearch-based indexing and querying of financial
 * transactions stored as Redis hashes.</p>
 */
public interface TransactionSearchPort {

    /**
     * Indexes a transaction as a Redis hash for RediSearch queries.
     *
     * @param tx the transaction to index
     */
    void indexTransaction(Transaction tx);

    /**
     * Searches indexed transactions using a RediSearch query.
     *
     * @param query the RediSearch query string
     * @return list of matching results as JSON strings
     */
    List<String> search(String query);
}
