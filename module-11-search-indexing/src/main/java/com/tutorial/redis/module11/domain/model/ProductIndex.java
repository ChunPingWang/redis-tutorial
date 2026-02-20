package com.tutorial.redis.module11.domain.model;

import java.util.Objects;

/**
 * Represents a product stored as a Redis Hash for RediSearch indexing.
 *
 * <p>Fields map to RediSearch schema field types:</p>
 * <ul>
 *   <li>{@code name} and {@code description} — TEXT fields (full-text searchable)</li>
 *   <li>{@code category} and {@code brand} — TAG fields (exact-match filtering)</li>
 *   <li>{@code price} — NUMERIC field (range queries)</li>
 * </ul>
 *
 * Mutable model with no-arg and all-args constructors for Redis Hash serialization.
 */
public class ProductIndex {

    private String productId;
    private String name;
    private String description;
    private String category;
    private double price;
    private String brand;

    public ProductIndex() {
    }

    public ProductIndex(String productId, String name, String description,
                        String category, double price, String brand) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.brand = brand;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductIndex that)) return false;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "ProductIndex{productId='%s', name='%s', category='%s', price=%s, brand='%s'}".formatted(
                productId, name, category, price, brand);
    }
}
