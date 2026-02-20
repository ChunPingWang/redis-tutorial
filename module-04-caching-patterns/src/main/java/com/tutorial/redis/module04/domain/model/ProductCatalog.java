package com.tutorial.redis.module04.domain.model;

import java.util.Objects;

/**
 * Represents a product in the catalog.
 * Used for Read-Through / Write-Through and Refresh-Ahead pattern demos.
 * In Read-Through the cache layer transparently loads data from the data
 * source on a miss; in Write-Through every write updates both cache and
 * data source synchronously.
 * Immutable value object — all fields are final.
 */
public class ProductCatalog {

    private final String productId;
    private final String name;
    private final String category;
    private final double price;
    private final String description;

    public ProductCatalog(String productId, String name, String category,
                          double price, String description) {
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        if (price < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
        this.price = price;
        this.description = description;
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductCatalog that)) return false;
        return productId.equals(that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "ProductCatalog{productId='%s', name='%s', category='%s', price=%f}".formatted(
                productId, name, category, price);
    }
}
