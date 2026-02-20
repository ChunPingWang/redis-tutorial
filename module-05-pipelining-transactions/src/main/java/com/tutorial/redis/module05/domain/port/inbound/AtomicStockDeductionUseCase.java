package com.tutorial.redis.module05.domain.port.inbound;

import com.tutorial.redis.module05.domain.model.StockDeductionResult;

import java.util.Optional;

/**
 * Inbound port: atomic stock deduction operations using Redis Lua scripts.
 * Lua scripts execute atomically on the Redis server, providing a race-condition-free
 * check-and-deduct mechanism to prevent overselling.
 */
public interface AtomicStockDeductionUseCase {

    /**
     * Atomically deducts stock for a product using a Lua script.
     * The script checks if sufficient stock exists before deducting.
     *
     * @param productId the product whose stock to deduct
     * @param quantity  the quantity to deduct (must be positive)
     * @return the result indicating success, insufficient stock, or key not found
     */
    StockDeductionResult deductStock(String productId, int quantity);

    /**
     * Initializes or resets the stock level for a product.
     *
     * @param productId the product to initialize
     * @param quantity  the stock quantity to set
     */
    void initializeStock(String productId, long quantity);

    /**
     * Retrieves the current stock level for a product.
     *
     * @param productId the product to query
     * @return the current stock quantity, or empty if the key does not exist
     */
    Optional<Long> getStock(String productId);
}
