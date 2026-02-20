package com.tutorial.redis.module05.domain.model;

import java.util.Objects;

/**
 * Represents the outcome of an atomic account-to-account transfer
 * executed via Redis MULTI/EXEC transaction.
 * Contains the source and target account IDs, the transfer amount,
 * and whether the transfer succeeded.
 * Immutable value object — all fields are final.
 */
public class TransferResult {

    private final String fromAccountId;
    private final String toAccountId;
    private final double amount;
    private final boolean success;
    private final String message;

    public TransferResult(String fromAccountId, String toAccountId, double amount,
                          boolean success, String message) {
        this.fromAccountId = Objects.requireNonNull(fromAccountId, "fromAccountId must not be null");
        this.toAccountId = Objects.requireNonNull(toAccountId, "toAccountId must not be null");
        this.message = Objects.requireNonNull(message, "message must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.amount = amount;
        this.success = success;
    }

    public String getFromAccountId() { return fromAccountId; }
    public String getToAccountId() { return toAccountId; }
    public double getAmount() { return amount; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransferResult that)) return false;
        return Double.compare(that.amount, amount) == 0
                && success == that.success
                && fromAccountId.equals(that.fromAccountId)
                && toAccountId.equals(that.toAccountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromAccountId, toAccountId, amount, success);
    }

    @Override
    public String toString() {
        return "TransferResult{fromAccountId='%s', toAccountId='%s', amount=%.2f, success=%b, message='%s'}".formatted(
                fromAccountId, toAccountId, amount, success, message);
    }
}
