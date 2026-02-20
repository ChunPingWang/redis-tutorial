package com.tutorial.redis.module02.domain.model;

import java.util.Objects;

/**
 * Represents an inventory stock level for a product.
 * Maps to Redis String structure (used as an atomic counter).
 * Immutable value object — all fields are final.
 */
public class StockLevel {

    private final String productId;
    private final long quantity;

    public StockLevel(String productId, long quantity) {
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public long getQuantity() { return quantity; }

    /**
     * Creates a new StockLevel with the updated quantity.
     */
    public StockLevel withQuantity(long quantity) {
        return new StockLevel(this.productId, quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockLevel that)) return false;
        return productId.equals(that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "StockLevel{productId='%s', quantity=%d}".formatted(productId, quantity);
    }
}
