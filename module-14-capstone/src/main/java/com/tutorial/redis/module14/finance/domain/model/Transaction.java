package com.tutorial.redis.module14.finance.domain.model;

/**
 * Financial transaction between two accounts.
 *
 * <p>Represents a monetary transfer from one account to another, including
 * the amount, currency, timestamp, and processing status. Transactions are
 * indexed in RediSearch for full-text queries and ranked in a sorted set
 * leaderboard by amount.</p>
 */
public class Transaction {

    private String transactionId;
    private String fromAccountId;
    private String toAccountId;
    private double amount;
    private String currency;
    private long timestamp;
    private String status;

    public Transaction() {
    }

    public Transaction(String transactionId, String fromAccountId, String toAccountId,
                       double amount, String currency, long timestamp, String status) {
        this.transactionId = transactionId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(String fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(String toAccountId) {
        this.toAccountId = toAccountId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Transaction{transactionId='" + transactionId + "', fromAccountId='"
                + fromAccountId + "', toAccountId='" + toAccountId + "', amount="
                + amount + ", currency='" + currency + "', timestamp=" + timestamp
                + ", status='" + status + "'}";
    }
}
