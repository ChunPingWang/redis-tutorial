package com.tutorial.redis.module02.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a financial transaction in an account's transaction log.
 * Maps to Redis List structure (ordered transaction history).
 * Immutable value object — all fields are final.
 */
public class Transaction {

    /**
     * The type of financial transaction.
     */
    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER
    }

    private final String transactionId;
    private final String accountId;
    private final BigDecimal amount;
    private final TransactionType type;
    private final Instant timestamp;
    private final String description;

    public Transaction(String transactionId, String accountId, BigDecimal amount,
                       TransactionType type, Instant timestamp, String description) {
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.description = description;
    }

    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public Instant getTimestamp() { return timestamp; }
    public String getDescription() { return description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return transactionId.equals(that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{transactionId='%s', accountId='%s', amount=%s, type=%s, timestamp=%s}".formatted(
                transactionId, accountId, amount, type, timestamp);
    }
}
