package com.tutorial.redis.module11.domain.model;

import java.util.Objects;

/**
 * Represents a financial transaction stored as a Redis Hash for RediSearch indexing.
 *
 * <p>Fields map to RediSearch schema field types:</p>
 * <ul>
 *   <li>{@code accountId} and {@code type} — TAG fields (exact-match filtering)</li>
 *   <li>{@code description} — TEXT field (full-text searchable)</li>
 *   <li>{@code amount} and {@code createdAt} — NUMERIC fields (range queries)</li>
 * </ul>
 *
 * <p>The {@code type} field expects values: DEPOSIT, WITHDRAWAL, or TRANSFER.</p>
 *
 * Mutable model with no-arg and all-args constructors for Redis Hash serialization.
 */
public class TransactionIndex {

    private String transactionId;
    private String accountId;
    private double amount;
    private String type;
    private String description;
    private long createdAt;

    public TransactionIndex() {
    }

    public TransactionIndex(String transactionId, String accountId, double amount,
                            String type, String description, long createdAt) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionIndex that)) return false;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "TransactionIndex{transactionId='%s', accountId='%s', amount=%s, type='%s', createdAt=%d}".formatted(
                transactionId, accountId, amount, type, createdAt);
    }
}
