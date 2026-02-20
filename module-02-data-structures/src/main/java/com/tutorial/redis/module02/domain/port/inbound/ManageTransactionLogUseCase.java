package com.tutorial.redis.module02.domain.port.inbound;

import com.tutorial.redis.module02.domain.model.Transaction;

import java.util.List;

/**
 * Inbound port: manage transaction logs using Redis List structure.
 */
public interface ManageTransactionLogUseCase {

    void recordTransaction(String accountId, Transaction transaction);

    List<Transaction> getRecentTransactions(String accountId, int count);

    long getTransactionCount(String accountId);
}
