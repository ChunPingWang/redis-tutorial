package com.tutorial.redis.module11.domain.port.outbound;

import com.tutorial.redis.module11.domain.model.ProductIndex;

import java.util.List;

/**
 * Outbound port for persisting product data as Redis Hashes.
 *
 * <p>Products are stored as Hashes so that RediSearch can index them.
 * The key format is typically {@code product:{productId}}.</p>
 */
public interface ProductDataPort {

    /**
     * Saves a single product as a Redis Hash.
     *
     * @param product the product to save
     */
    void saveProduct(ProductIndex product);

    /**
     * Saves multiple products as Redis Hashes in a batch.
     *
     * @param products the list of products to save
     */
    void saveProducts(List<ProductIndex> products);
}
