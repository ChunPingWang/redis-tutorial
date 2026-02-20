package com.tutorial.redis.module06.domain.port.outbound;

import com.tutorial.redis.module06.domain.model.ExchangeRateSnapshot;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for exchange rate time-series operations.
 * Models time-series data using Redis Sorted Sets where the score
 * is the epoch-millisecond timestamp, enabling range queries via ZRANGEBYSCORE.
 *
 * Key schema: {@code exchange-service:rate:{currencyPair}}
 */
public interface ExchangeRateTimeSeriesPort {

    /**
     * Adds an exchange rate snapshot to the time series.
     * The snapshot's timestamp is used as the Sorted Set score.
     */
    void addSnapshot(ExchangeRateSnapshot snapshot);

    /**
     * Retrieves all snapshots for a currency pair within the given
     * epoch-millisecond range (inclusive) using ZRANGEBYSCORE.
     */
    List<ExchangeRateSnapshot> getSnapshots(String currencyPair, long fromEpoch, long toEpoch);

    /**
     * Retrieves the most recent snapshot for a currency pair
     * using ZREVRANGEBYSCORE with a limit of 1.
     */
    Optional<ExchangeRateSnapshot> getLatestSnapshot(String currencyPair);
}
