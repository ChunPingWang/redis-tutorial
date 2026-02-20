package com.tutorial.redis.module04.domain.port.outbound;

import com.tutorial.redis.module04.domain.model.ExchangeRate;

import java.util.Optional;

/**
 * Outbound port for retrieving exchange rates from the authoritative
 * data source (database or external API).
 * In the Cache-Aside pattern the application falls back to this port
 * when the cache does not contain the requested currency pair.
 * Implemented by a persistence or API adapter in the infrastructure layer.
 */
public interface ExchangeRateRepositoryPort {

    /**
     * Fetches the current exchange rate for a currency pair from the data source.
     *
     * @param pair the currency pair (e.g. "USD/TWD")
     * @return the exchange rate, or empty if the pair is not supported
     */
    Optional<ExchangeRate> findByPair(String pair);
}
