package com.tutorial.redis.module14.ecommerce.domain.model;

/**
 * Product aggregate.
 *
 * <p>Represents a product in the e-commerce catalog with name, category,
 * price, description, and stock information. Used for search indexing,
 * caching, and order processing.</p>
 */
public class Product {

    private String productId;
    private String name;
    private String category;
    private double price;
    private String description;
    private int stockQuantity;

    public Product() {
    }

    public Product(String productId, String name, String category,
                   double price, String description, int stockQuantity) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.stockQuantity = stockQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    @Override
    public String toString() {
        return "Product{productId='" + productId + "', name='" + name
                + "', category='" + category + "', price=" + price
                + ", description='" + description + "', stockQuantity=" + stockQuantity + '}';
    }
}
