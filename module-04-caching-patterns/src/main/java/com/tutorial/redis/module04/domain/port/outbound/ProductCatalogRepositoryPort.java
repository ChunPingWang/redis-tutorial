package com.tutorial.redis.module04.domain.port.outbound;

import com.tutorial.redis.module04.domain.model.ProductCatalog;

import java.util.Optional;

/**
 * Outbound port for retrieving products from the authoritative data source.
 * Used by Read-Through and Refresh-Ahead patterns to load product data
 * when the cache entry is missing or about to expire.
 * Implemented by a persistence adapter in the infrastructure layer.
 */
public interface ProductCatalogRepositoryPort {

    /**
     * Fetches a product by its identifier from the data source.
     *
     * @param productId the product identifier
     * @return the product, or empty if not found
     */
    Optional<ProductCatalog> findById(String productId);
}
