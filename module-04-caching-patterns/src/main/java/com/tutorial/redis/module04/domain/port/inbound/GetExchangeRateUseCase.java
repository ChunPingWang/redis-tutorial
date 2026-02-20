package com.tutorial.redis.module04.domain.port.inbound;

import com.tutorial.redis.module04.domain.model.ExchangeRate;

/**
 * Inbound port: retrieve an exchange rate using the Cache-Aside pattern.
 * The implementation first checks the cache; on a miss it fetches from
 * the data source, writes the result back to the cache, and returns it.
 */
public interface GetExchangeRateUseCase {

    /**
     * Returns the exchange rate for the given currency pair.
     * Applies the Cache-Aside (Lazy Loading) strategy.
     *
     * @param currencyPair the currency pair (e.g. "USD/TWD")
     * @return the exchange rate
     * @throws IllegalArgumentException if the currency pair is not supported
     */
    ExchangeRate getRate(String currencyPair);
}
