package com.tutorial.redis.module02.application.dto;

/**
 * Response DTO for stock level queries.
 */
public record StockLevelResponse(
        String productId,
        long quantity
) {
}
