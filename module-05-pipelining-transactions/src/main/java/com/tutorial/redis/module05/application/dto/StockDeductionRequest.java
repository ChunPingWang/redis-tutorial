package com.tutorial.redis.module05.application.dto;

/**
 * Request DTO for atomic stock deduction operations.
 *
 * @param productId the product whose stock to deduct
 * @param quantity  the quantity to deduct (must be positive)
 */
public record StockDeductionRequest(
        String productId,
        int quantity
) {
}
