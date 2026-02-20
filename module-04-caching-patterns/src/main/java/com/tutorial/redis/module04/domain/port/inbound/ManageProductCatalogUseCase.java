package com.tutorial.redis.module04.domain.port.inbound;

import com.tutorial.redis.module04.domain.model.ProductCatalog;

import java.util.Optional;

/**
 * Inbound port: manage products using Read-Through / Write-Through caching.
 * Reads go through the cache transparently; writes update both cache and
 * data source to maintain consistency.
 */
public interface ManageProductCatalogUseCase {

    /**
     * Retrieves a product by its identifier.
     * Applies Read-Through: the cache is checked first and populated
     * automatically on a miss.
     *
     * @param productId the product identifier
     * @return the product, or empty if not found in cache or data source
     */
    Optional<ProductCatalog> getProduct(String productId);

    /**
     * Saves a product using the Write-Through strategy.
     * The product is written to both the cache and the data source.
     *
     * @param product the product to save
     */
    void saveProduct(ProductCatalog product);

    /**
     * Evicts a product from the cache by its identifier.
     *
     * @param productId the product identifier to evict
     */
    void evictProduct(String productId);
}
