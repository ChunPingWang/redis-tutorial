package com.tutorial.redis.module06.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents an account aggregate for the DAO pattern.
 * Demonstrates full entity storage in Redis using multiple strategies:
 * JSON String, Hash per entity, and Multi-Key decomposition.
 *
 * Key schema: {@code account-service:account:{accountId}}
 * Secondary indexes: by currency (Set), by status (Set).
 */
public class AccountAggregate {

    private final String accountId;
    private final String holderName;
    private final double balance;
    private final String currency;
    private final Instant createdAt;
    private final String status;

    public AccountAggregate(String accountId, String holderName, double balance,
                            String currency, Instant createdAt, String status) {
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.holderName = Objects.requireNonNull(holderName, "holderName must not be null");
        this.balance = balance;
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public String getAccountId() { return accountId; }
    public String getHolderName() { return holderName; }
    public double getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountAggregate that)) return false;
        return accountId.equals(that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }

    @Override
    public String toString() {
        return "AccountAggregate{accountId='%s', holderName='%s', balance=%.2f, currency='%s', status='%s'}".formatted(
                accountId, holderName, balance, currency, status);
    }
}
