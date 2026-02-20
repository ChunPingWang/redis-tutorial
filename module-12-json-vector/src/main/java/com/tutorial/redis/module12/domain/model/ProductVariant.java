package com.tutorial.redis.module12.domain.model;

import java.util.Objects;

/**
 * Represents a variant of a product stored within a RedisJSON document.
 *
 * <p>Each variant captures a distinct SKU with its own color, price, and stock level.
 * Variants are nested inside {@link ProductDocument} and manipulated via
 * JSON.ARRAPPEND Lua scripts.</p>
 *
 * Mutable model with no-arg and all-args constructors.
 */
public class ProductVariant {

    private String sku;
    private String color;
    private double price;
    private int stock;

    public ProductVariant() {
    }

    public ProductVariant(String sku, String color, double price, int stock) {
        this.sku = sku;
        this.color = color;
        this.price = price;
        this.stock = stock;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductVariant that)) return false;
        return Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }

    @Override
    public String toString() {
        return "ProductVariant{sku='%s', color='%s', price=%s, stock=%d}".formatted(
                sku, color, price, stock);
    }
}
