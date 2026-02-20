package com.tutorial.redis.module02.application.usecase;

import com.tutorial.redis.module02.domain.model.Transaction;
import com.tutorial.redis.module02.domain.port.inbound.ManageTransactionLogUseCase;
import com.tutorial.redis.module02.domain.port.outbound.TransactionLogPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service implementing transaction log management use cases.
 *
 * <p>Delegates to {@link TransactionLogPort} for Redis List operations.
 * Demonstrates the CAPPED COLLECTION pattern: after each insert, the list
 * is trimmed to {@code MAX_LOG_SIZE} to prevent unbounded growth.</p>
 */
@Service
public class ManageTransactionLogService implements ManageTransactionLogUseCase {

    static final int MAX_LOG_SIZE = 100;

    private final TransactionLogPort transactionLogPort;

    public ManageTransactionLogService(TransactionLogPort transactionLogPort) {
        this.transactionLogPort = transactionLogPort;
    }

    @Override
    public void recordTransaction(String accountId, Transaction transaction) {
        transactionLogPort.addTransaction(accountId, transaction);
        transactionLogPort.trimToSize(accountId, MAX_LOG_SIZE);
    }

    @Override
    public List<Transaction> getRecentTransactions(String accountId, int count) {
        return transactionLogPort.getRecentTransactions(accountId, count);
    }

    @Override
    public long getTransactionCount(String accountId) {
        return transactionLogPort.getTransactionCount(accountId);
    }
}
