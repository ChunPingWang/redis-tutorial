package com.tutorial.redis.module04.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a financial transaction event to be buffered and batch-persisted.
 * Used for the Write-Behind (Write-Back) pattern demo where events are
 * first written to a Redis List as a temporary buffer, then periodically
 * flushed in batches to the underlying data store.
 * Immutable value object — all fields are final.
 */
public class TransactionEvent {

    private final String transactionId;
    private final String accountId;
    private final double amount;
    private final String type;
    private final Instant timestamp;

    public TransactionEvent(String transactionId, String accountId, double amount,
                            String type, Instant timestamp) {
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        if (!type.equals("DEPOSIT") && !type.equals("WITHDRAWAL")) {
            throw new IllegalArgumentException("type must be DEPOSIT or WITHDRAWAL");
        }
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.amount = amount;
    }

    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionEvent that)) return false;
        return transactionId.equals(that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "TransactionEvent{transactionId='%s', accountId='%s', amount=%f, type='%s', timestamp=%s}".formatted(
                transactionId, accountId, amount, type, timestamp);
    }
}
