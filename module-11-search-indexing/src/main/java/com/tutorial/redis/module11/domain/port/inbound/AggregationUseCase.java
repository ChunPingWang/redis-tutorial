package com.tutorial.redis.module11.domain.port.inbound;

import com.tutorial.redis.module11.domain.model.AggregationResult;

/**
 * Inbound port for RediSearch aggregation operations.
 *
 * <p>Provides use cases for running FT.AGGREGATE queries with
 * GROUPBY and REDUCE operations against indexed data.</p>
 */
public interface AggregationUseCase {

    /**
     * Aggregates average product price grouped by category.
     *
     * @param indexName the name of the product search index
     * @return aggregation result with category and average price per row
     */
    AggregationResult aggregateAveragePriceByCategory(String indexName);
}
