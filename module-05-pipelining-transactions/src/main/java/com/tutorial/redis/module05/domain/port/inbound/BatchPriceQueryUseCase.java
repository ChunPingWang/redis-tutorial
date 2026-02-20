package com.tutorial.redis.module05.domain.port.inbound;

import java.util.List;
import java.util.Map;

/**
 * Inbound port: batch price query and update operations using Redis pipelines.
 * Pipelines reduce RTT by batching multiple GET/SET commands into a single round-trip.
 */
public interface BatchPriceQueryUseCase {

    /**
     * Retrieves prices for multiple products in a single pipeline batch.
     * Products not found in the cache will have a null value in the returned map.
     *
     * @param productIds the list of product IDs to query
     * @return a map of product ID to price (null if not found)
     */
    Map<String, Double> batchGetPrices(List<String> productIds);

    /**
     * Sets prices for multiple products in a single pipeline batch.
     *
     * @param prices a map of product ID to price
     */
    void batchSetPrices(Map<String, Double> prices);
}
