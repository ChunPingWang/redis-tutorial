package com.tutorial.redis.module05.domain.model;

import java.util.Objects;

/**
 * Represents the result of a pipeline batch price query for a single product.
 * The price may be null if the product was not found in the cache.
 * Value object used in pipeline batch GET operations to reduce RTT.
 * Immutable value object — all fields are final.
 */
public class BatchPriceResult {

    private final String productId;
    private final Double price;

    public BatchPriceResult(String productId, Double price) {
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        this.price = price;
    }

    public String getProductId() { return productId; }
    public Double getPrice() { return price; }

    /**
     * Returns true if the product was found and has a price.
     */
    public boolean isFound() {
        return price != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BatchPriceResult that)) return false;
        return productId.equals(that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "BatchPriceResult{productId='%s', price=%s}".formatted(productId, price);
    }
}
