package com.tutorial.redis.module02.domain.service;

import com.tutorial.redis.module02.domain.model.CartItem;
import com.tutorial.redis.module02.domain.model.ShoppingCart;

import java.math.BigDecimal;

/**
 * Domain service for shopping cart business rules.
 * Pure domain logic — zero framework dependency.
 */
public class CartService {

    static final int MAX_CART_ITEMS = 50;
    static final int MAX_ITEM_QUANTITY = 99;

    /**
     * Validates that a quantity is within the allowed range.
     */
    public boolean isValidQuantity(int quantity) {
        return quantity >= 1 && quantity <= MAX_ITEM_QUANTITY;
    }

    /**
     * Checks if the cart has reached its maximum item capacity.
     */
    public boolean isCartFull(ShoppingCart cart) {
        return cart.itemCount() >= MAX_CART_ITEMS;
    }

    /**
     * Calculates the total amount for all items in the cart.
     */
    public BigDecimal calculateCartTotal(ShoppingCart cart) {
        return cart.getItems().values().stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
