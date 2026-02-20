package com.tutorial.redis.module12.domain.port.inbound;

import com.tutorial.redis.module12.domain.model.ProductDocument;
import com.tutorial.redis.module12.domain.model.ProductVariant;

/**
 * Inbound port for RedisJSON product document use cases.
 *
 * <p>Exposes high-level operations for managing product documents stored
 * as nested JSON in Redis. Each operation maps to one or more RedisJSON
 * commands (JSON.SET, JSON.GET, JSON.DEL, JSON.NUMINCRBY, JSON.ARRAPPEND)
 * executed through Lua scripts.</p>
 */
public interface JsonDocumentUseCase {

    /**
     * Saves a product document as a JSON value in Redis.
     *
     * @param product the product document to persist
     */
    void saveProduct(ProductDocument product);

    /**
     * Retrieves a product document by its identifier.
     *
     * @param productId the unique product identifier
     * @return the product document, or {@code null} if not found
     */
    ProductDocument getProduct(String productId);

    /**
     * Deletes a product document from Redis.
     *
     * @param productId the unique product identifier
     */
    void deleteProduct(String productId);

    /**
     * Updates the price of an existing product document atomically.
     *
     * @param productId the unique product identifier
     * @param newPrice  the new price value to set
     */
    void updatePrice(String productId, double newPrice);

    /**
     * Appends a new variant to the product's variants array.
     *
     * @param productId the unique product identifier
     * @param variant   the product variant to append
     */
    void addVariant(String productId, ProductVariant variant);
}
