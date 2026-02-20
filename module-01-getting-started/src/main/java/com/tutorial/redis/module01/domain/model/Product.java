package com.tutorial.redis.module01.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * E-commerce product entity for cache demonstration.
 * Immutable value object — all fields are final.
 */
public class Product {

    private final String productId;
    private final String name;
    private final BigDecimal price;
    private final String category;
    private final int stockQuantity;

    public Product(String productId, String name, BigDecimal price,
                   String category, int stockQuantity) {
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.price = Objects.requireNonNull(price, "price must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        if (stockQuantity < 0) throw new IllegalArgumentException("stockQuantity must not be negative");
        this.stockQuantity = stockQuantity;
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category; }
    public int getStockQuantity() { return stockQuantity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product that)) return false;
        return productId.equals(that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "Product{productId='%s', name='%s', price=%s, category='%s'}".formatted(
                productId, name, price, category);
    }
}
