package com.tutorial.redis.module02.application.dto;

import com.tutorial.redis.module02.domain.model.CartItem;

import java.math.BigDecimal;

/**
 * Response DTO for individual cart items.
 */
public record CartItemResponse(
        String productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.subtotal()
        );
    }
}
