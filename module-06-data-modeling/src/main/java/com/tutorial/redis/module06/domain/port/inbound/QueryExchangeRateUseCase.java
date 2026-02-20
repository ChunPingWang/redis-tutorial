package com.tutorial.redis.module06.domain.port.inbound;

import com.tutorial.redis.module06.domain.model.ExchangeRateSnapshot;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: record and query exchange rate time-series data.
 * Snapshots are modeled in Redis using Sorted Sets with
 * timestamp-based scores for efficient range queries.
 */
public interface QueryExchangeRateUseCase {

    /**
     * Records a new exchange rate snapshot into the time series.
     */
    void recordRate(ExchangeRateSnapshot snapshot);

    /**
     * Queries exchange rate snapshots for a currency pair
     * within the given epoch-millisecond range.
     */
    List<ExchangeRateSnapshot> queryRates(String currencyPair, long fromEpoch, long toEpoch);

    /**
     * Retrieves the latest (most recent) exchange rate for a currency pair.
     */
    Optional<ExchangeRateSnapshot> getLatestRate(String currencyPair);
}
