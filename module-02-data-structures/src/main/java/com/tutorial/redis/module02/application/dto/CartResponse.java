package com.tutorial.redis.module02.application.dto;

import com.tutorial.redis.module02.domain.model.ShoppingCart;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for a complete shopping cart.
 */
public record CartResponse(
        String customerId,
        List<CartItemResponse> items,
        BigDecimal totalAmount,
        int itemCount
) {
    public static CartResponse from(ShoppingCart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().values().stream()
                .map(CartItemResponse::from)
                .toList();

        return new CartResponse(
                cart.getCustomerId(),
                itemResponses,
                cart.totalAmount(),
                cart.itemCount()
        );
    }
}
