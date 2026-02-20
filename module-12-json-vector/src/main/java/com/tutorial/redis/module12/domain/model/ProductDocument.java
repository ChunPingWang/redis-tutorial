package com.tutorial.redis.module12.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a nested product document stored in Redis via RedisJSON.
 *
 * <p>This model mirrors the JSON structure persisted with JSON.SET and retrieved
 * with JSON.GET. It contains nested {@link ProductVariant} items and a
 * {@link ReviewSummary}, demonstrating RedisJSON's native support for rich,
 * hierarchical documents.</p>
 *
 * <p>Key RedisJSON operations mapped to this model:</p>
 * <ul>
 *   <li>JSON.SET — save or replace the entire document</li>
 *   <li>JSON.GET — retrieve the document (or a sub-path)</li>
 *   <li>JSON.DEL — delete the document</li>
 *   <li>JSON.NUMINCRBY — atomically adjust {@code price}</li>
 *   <li>JSON.ARRAPPEND — append a new {@link ProductVariant} to {@code variants}</li>
 * </ul>
 *
 * Mutable model with no-arg and all-args constructors.
 */
public class ProductDocument {

    private String productId;
    private String name;
    private String description;
    private double price;
    private String category;
    private List<ProductVariant> variants;
    private ReviewSummary reviews;

    public ProductDocument() {
        this.variants = new ArrayList<>();
    }

    public ProductDocument(String productId, String name, String description,
                           double price, String category,
                           List<ProductVariant> variants, ReviewSummary reviews) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.variants = variants != null ? variants : new ArrayList<>();
        this.reviews = reviews;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<ProductVariant> getVariants() { return variants; }
    public void setVariants(List<ProductVariant> variants) { this.variants = variants; }

    public ReviewSummary getReviews() { return reviews; }
    public void setReviews(ReviewSummary reviews) { this.reviews = reviews; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductDocument that)) return false;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "ProductDocument{productId='%s', name='%s', category='%s', price=%s, variantCount=%d}".formatted(
                productId, name, category, price, variants.size());
    }
}
