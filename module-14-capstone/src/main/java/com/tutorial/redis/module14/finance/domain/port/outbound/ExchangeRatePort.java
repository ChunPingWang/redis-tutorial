package com.tutorial.redis.module14.finance.domain.port.outbound;

/**
 * Outbound port for exchange rate time-series operations.
 *
 * <p>Abstracts the Redis TimeSeries module used to record and retrieve
 * currency exchange rates over time.</p>
 */
public interface ExchangeRatePort {

    /**
     * Records an exchange rate data point in Redis TimeSeries.
     *
     * @param pair      the currency pair (e.g. "USD/EUR")
     * @param rate      the exchange rate value
     * @param timestamp the Unix timestamp in milliseconds
     */
    void recordRate(String pair, double rate, long timestamp);

    /**
     * Retrieves the latest exchange rate for a currency pair.
     *
     * @param pair the currency pair (e.g. "USD/EUR")
     * @return the latest rate, or {@code null} if no data exists
     */
    Double getLatestRate(String pair);
}
