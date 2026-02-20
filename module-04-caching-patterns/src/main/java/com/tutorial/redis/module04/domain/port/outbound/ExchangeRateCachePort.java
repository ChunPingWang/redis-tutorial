package com.tutorial.redis.module04.domain.port.outbound;

import com.tutorial.redis.module04.domain.model.ExchangeRate;

import java.util.Optional;

/**
 * Outbound port for exchange rate cache operations.
 * Supports the Cache-Aside pattern where the application explicitly
 * manages reading from and writing to the cache.
 * Implemented by a Redis adapter in the infrastructure layer.
 */
public interface ExchangeRateCachePort {

    /**
     * Stores an exchange rate in the cache.
     */
    void save(ExchangeRate exchangeRate);

    /**
     * Looks up a cached exchange rate by currency pair (e.g. "USD/TWD").
     *
     * @return the cached rate, or empty on a cache miss
     */
    Optional<ExchangeRate> findByPair(String pair);

    /**
     * Removes a cached exchange rate by currency pair.
     */
    void evict(String pair);
}
