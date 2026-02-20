package com.tutorial.redis.module14.finance.domain.model;

/**
 * Currency exchange rate data point.
 *
 * <p>Represents a single exchange rate observation for a currency pair
 * at a specific point in time. Exchange rates are stored in Redis
 * TimeSeries for historical tracking and retrieval of the latest rate.</p>
 */
public class ExchangeRate {

    private String currencyPair;
    private double rate;
    private long timestamp;

    public ExchangeRate() {
    }

    public ExchangeRate(String currencyPair, double rate, long timestamp) {
        this.currencyPair = currencyPair;
        this.rate = rate;
        this.timestamp = timestamp;
    }

    public String getCurrencyPair() {
        return currencyPair;
    }

    public void setCurrencyPair(String currencyPair) {
        this.currencyPair = currencyPair;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ExchangeRate{currencyPair='" + currencyPair + "', rate=" + rate
                + ", timestamp=" + timestamp + '}';
    }
}
