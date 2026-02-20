package com.tutorial.redis.module05.domain.port.outbound;

import com.tutorial.redis.module05.domain.model.StockDeductionResult;

import java.util.Optional;

/**
 * Outbound port for atomic stock deduction using a Redis Lua script.
 * The Lua script atomically checks and deducts stock to prevent overselling.
 * Implemented by a Redis adapter in the infrastructure layer.
 */
public interface AtomicStockDeductionPort {

    /**
     * Atomically checks if sufficient stock exists and deducts the requested quantity
     * using a server-side Lua script. This guarantees no race conditions.
     *
     * @param productId the product whose stock to deduct
     * @param quantity  the quantity to deduct (must be positive)
     * @return the result indicating success, insufficient stock, or key not found
     */
    StockDeductionResult deductStock(String productId, int quantity);

    /**
     * Retrieves the current stock level for a product.
     *
     * @param productId the product to query
     * @return the current stock quantity, or empty if the key does not exist
     */
    Optional<Long> getStock(String productId);

    /**
     * Initializes or resets the stock level for a product.
     *
     * @param productId the product to initialize
     * @param quantity  the stock quantity to set
     */
    void setStock(String productId, long quantity);
}
