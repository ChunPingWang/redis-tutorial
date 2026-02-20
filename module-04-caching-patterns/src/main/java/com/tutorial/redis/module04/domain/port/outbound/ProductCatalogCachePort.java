package com.tutorial.redis.module04.domain.port.outbound;

import com.tutorial.redis.module04.domain.model.ProductCatalog;

import java.util.Optional;

/**
 * Outbound port for product catalog cache operations.
 * Supports Read-Through / Write-Through and Refresh-Ahead patterns.
 * In Read-Through the cache transparently loads on a miss; in
 * Write-Through every write goes to both cache and data source.
 * Implemented by a Redis adapter in the infrastructure layer.
 */
public interface ProductCatalogCachePort {

    /**
     * Stores a product in the cache with a specific time-to-live.
     *
     * @param product the product to cache
     * @param ttlMs   time-to-live in milliseconds
     */
    void save(ProductCatalog product, long ttlMs);

    /**
     * Looks up a cached product by its identifier.
     *
     * @return the cached product, or empty on a cache miss
     */
    Optional<ProductCatalog> findById(String productId);

    /**
     * Removes a product from the cache.
     */
    void evict(String productId);

    /**
     * Checks whether a product exists in the cache without retrieving it.
     */
    boolean exists(String productId);
}
