package com.tutorial.redis.module02.domain.port.outbound;

import com.tutorial.redis.module02.domain.model.Transaction;

import java.util.List;

/**
 * Outbound port for transaction log operations.
 * Uses Redis List structure (ordered transaction history).
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface TransactionLogPort {

    void addTransaction(String accountId, Transaction transaction);

    List<Transaction> getRecentTransactions(String accountId, int count);

    void trimToSize(String accountId, int maxSize);

    long getTransactionCount(String accountId);
}
