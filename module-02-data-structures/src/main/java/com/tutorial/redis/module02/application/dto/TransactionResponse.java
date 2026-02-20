package com.tutorial.redis.module02.application.dto;

import com.tutorial.redis.module02.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for transaction log entries.
 */
public record TransactionResponse(
        String transactionId,
        String accountId,
        BigDecimal amount,
        String type,
        Instant timestamp,
        String description
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getTransactionId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getType().name(),
                transaction.getTimestamp(),
                transaction.getDescription()
        );
    }
}
