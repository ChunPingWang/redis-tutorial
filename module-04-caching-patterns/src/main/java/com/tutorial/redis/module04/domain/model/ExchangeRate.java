package com.tutorial.redis.module04.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a currency exchange rate at a point in time.
 * Value object used for the Cache-Aside (Lazy Loading) pattern demo.
 * The application checks the cache first; on a miss it fetches from the
 * data source, writes the result back to cache, and returns it.
 * Immutable value object — all fields are final.
 */
public class ExchangeRate {

    private final String currencyPair;
    private final double rate;
    private final Instant timestamp;

    public ExchangeRate(String currencyPair, double rate, Instant timestamp) {
        this.currencyPair = Objects.requireNonNull(currencyPair, "currencyPair must not be null");
        if (rate <= 0) {
            throw new IllegalArgumentException("rate must be positive");
        }
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.rate = rate;
    }

    public String getCurrencyPair() { return currencyPair; }
    public double getRate() { return rate; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExchangeRate that)) return false;
        return currencyPair.equals(that.currencyPair);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currencyPair);
    }

    @Override
    public String toString() {
        return "ExchangeRate{currencyPair='%s', rate=%f, timestamp=%s}".formatted(
                currencyPair, rate, timestamp);
    }
}
