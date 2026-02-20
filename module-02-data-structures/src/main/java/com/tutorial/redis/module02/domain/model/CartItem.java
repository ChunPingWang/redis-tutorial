package com.tutorial.redis.module02.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a single item in a shopping cart.
 * Immutable value object — all fields are final.
 */
public class CartItem {

    private final String productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;

    public CartItem(String productId, String productName, BigDecimal unitPrice, int quantity) {
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        this.productName = Objects.requireNonNull(productName, "productName must not be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("unitPrice must be greater than 0");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }

    /**
     * Calculates the subtotal for this item (unitPrice * quantity).
     */
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Creates a new CartItem with the updated quantity.
     */
    public CartItem withQuantity(int quantity) {
        return new CartItem(this.productId, this.productName, this.unitPrice, quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem that)) return false;
        return productId.equals(that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "CartItem{productId='%s', productName='%s', unitPrice=%s, quantity=%d}".formatted(
                productId, productName, unitPrice, quantity);
    }
}
