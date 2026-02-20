package com.tutorial.redis.module04.domain.port.inbound;

import com.tutorial.redis.module04.domain.model.ProductCatalog;

import java.util.Optional;

/**
 * Inbound port: retrieve a product using the Refresh-Ahead pattern.
 * When the remaining TTL of a cached entry falls below a threshold
 * (e.g. 20% of the original TTL), an asynchronous refresh is triggered
 * to proactively reload the entry before it expires, preventing cache misses.
 */
public interface RefreshAheadCacheUseCase {

    /**
     * Returns a cached product, triggering an asynchronous refresh if the
     * remaining TTL is below 20% of the original.
     *
     * @param productId the product identifier
     * @return the product, or empty if not found in cache or data source
     */
    Optional<ProductCatalog> getWithRefreshAhead(String productId);
}
