package com.tutorial.redis.module06.domain.model;

import java.util.Objects;

/**
 * Value object representing a single line item within an Order.
 * Contains product details, quantity, and unit price.
 * Immutable — all fields are final.
 */
public class OrderItem {

    private final String productId;
    private final String productName;
    private final int quantity;
    private final double unitPrice;

    public OrderItem(String productId, String productName, int quantity, double unitPrice) {
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        this.productName = Objects.requireNonNull(productName, "productName must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }

    /**
     * Calculates the subtotal for this line item.
     */
    public double subtotal() {
        return quantity * unitPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem that)) return false;
        return productId.equals(that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "OrderItem{productId='%s', productName='%s', quantity=%d, unitPrice=%.2f}".formatted(
                productId, productName, quantity, unitPrice);
    }
}
