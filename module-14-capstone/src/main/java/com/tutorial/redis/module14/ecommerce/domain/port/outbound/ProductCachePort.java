package com.tutorial.redis.module14.ecommerce.domain.port.outbound;

/**
 * Outbound port for product cache operations.
 *
 * <p>Abstracts Redis String operations for caching product JSON with
 * TTL-based expiration, retrieving cached products, and explicit
 * cache eviction.</p>
 */
public interface ProductCachePort {

    void cacheProduct(String productId, String productJson);

    String getCachedProduct(String productId);

    void evictProduct(String productId);
}
