package com.tutorial.redis.module14.ecommerce.domain.service;

import com.tutorial.redis.module14.ecommerce.domain.model.CartItem;

import java.util.List;

/**
 * Pure domain service for order total calculations.
 *
 * <p>Contains business logic for computing order totals from cart items
 * and applying percentage-based discounts. Has no infrastructure
 * dependencies and is fully testable in isolation.</p>
 */
public class OrderTotalCalculator {

    /**
     * Calculates the total amount for a list of cart items.
     *
     * @param items the cart items to sum
     * @return the total (sum of price * quantity for each item)
     */
    public double calculateTotal(List<CartItem> items) {
        double total = 0.0;
        for (CartItem item : items) {
            total += item.getPrice() * item.getQuantity();
        }
        return total;
    }

    /**
     * Applies a percentage discount to the given total.
     *
     * @param total           the original total amount
     * @param discountPercent the discount percentage (e.g., 10.0 for 10%)
     * @return the discounted total
     */
    public double applyDiscount(double total, double discountPercent) {
        return total * (1.0 - discountPercent / 100.0);
    }
}
