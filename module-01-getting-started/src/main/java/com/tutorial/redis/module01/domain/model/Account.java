package com.tutorial.redis.module01.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Banking account entity for cache demonstration.
 * Immutable value object — all fields are final.
 */
public class Account {

    private final String accountId;
    private final String holderName;
    private final BigDecimal balance;
    private final String currency;
    private final Instant lastUpdated;

    public Account(String accountId, String holderName, BigDecimal balance,
                   String currency, Instant lastUpdated) {
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.holderName = Objects.requireNonNull(holderName, "holderName must not be null");
        this.balance = Objects.requireNonNull(balance, "balance must not be null");
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.lastUpdated = Objects.requireNonNull(lastUpdated, "lastUpdated must not be null");
    }

    public String getAccountId() { return accountId; }
    public String getHolderName() { return holderName; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public Instant getLastUpdated() { return lastUpdated; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account that)) return false;
        return accountId.equals(that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }

    @Override
    public String toString() {
        return "Account{accountId='%s', holderName='%s', balance=%s %s}".formatted(
                accountId, holderName, balance, currency);
    }
}
