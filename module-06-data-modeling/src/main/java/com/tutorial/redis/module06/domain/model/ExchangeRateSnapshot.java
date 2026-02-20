package com.tutorial.redis.module06.domain.model;

import java.util.Objects;

/**
 * Represents a point-in-time exchange rate snapshot for time-series modeling.
 * Stored in Redis as a Sorted Set entry with timestamp as the score,
 * enabling efficient range queries via ZRANGEBYSCORE.
 *
 * Key schema: {@code exchange-service:rate:{currencyPair}}
 */
public class ExchangeRateSnapshot {

    private final String currencyPair;
    private final double rate;
    private final long timestamp;

    public ExchangeRateSnapshot(String currencyPair, double rate, long timestamp) {
        this.currencyPair = Objects.requireNonNull(currencyPair, "currencyPair must not be null");
        this.rate = rate;
        this.timestamp = timestamp;
    }

    public String getCurrencyPair() { return currencyPair; }
    public double getRate() { return rate; }
    public long getTimestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExchangeRateSnapshot that)) return false;
        return timestamp == that.timestamp && currencyPair.equals(that.currencyPair);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currencyPair, timestamp);
    }

    @Override
    public String toString() {
        return "ExchangeRateSnapshot{currencyPair='%s', rate=%.6f, timestamp=%d}".formatted(
                currencyPair, rate, timestamp);
    }
}
