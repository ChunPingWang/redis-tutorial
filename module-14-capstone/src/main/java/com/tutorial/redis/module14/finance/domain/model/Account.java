package com.tutorial.redis.module14.finance.domain.model;

/**
 * Bank account aggregate.
 *
 * <p>Represents a financial account with an owner, balance, and currency.
 * Used as the primary aggregate in the finance sub-system for caching
 * account balances and storing account profiles in RedisJSON.</p>
 */
public class Account {

    private String accountId;
    private String ownerName;
    private double balance;
    private String currency;
    private long createdAt;

    public Account() {
    }

    public Account(String accountId, String ownerName, double balance,
                   String currency, long createdAt) {
        this.accountId = accountId;
        this.ownerName = ownerName;
        this.balance = balance;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Account{accountId='" + accountId + "', ownerName='" + ownerName
                + "', balance=" + balance + ", currency='" + currency
                + "', createdAt=" + createdAt + '}';
    }
}
