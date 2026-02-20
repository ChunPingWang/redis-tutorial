package com.tutorial.redis.module05.domain.port.outbound;

import java.util.List;
import java.util.Map;

/**
 * Outbound port for Redis pipeline operations.
 * Pipelines batch multiple commands into a single round-trip to reduce RTT.
 * Implemented by a Redis adapter in the infrastructure layer.
 */
public interface PipelinePort {

    /**
     * Retrieves prices for multiple products in a single pipeline batch GET.
     * Products not found in the cache will have a null value in the returned map.
     *
     * @param productIds the list of product IDs to query
     * @return a map of product ID to price (null if not found)
     */
    Map<String, Double> batchGetPrices(List<String> productIds);

    /**
     * Sets prices for multiple products in a single pipeline batch SET.
     *
     * @param prices a map of product ID to price
     */
    void batchSetPrices(Map<String, Double> prices);
}
