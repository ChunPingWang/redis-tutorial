package com.tutorial.redis.module14.ecommerce.domain.model;

/**
 * Shopping cart item value object.
 *
 * <p>Represents a single item in a customer's shopping cart, containing
 * product reference, name, unit price, and quantity. Serialized as a
 * simple string representation for Redis Hash storage.</p>
 */
public class CartItem {

    private String productId;
    private String productName;
    private double price;
    private int quantity;

    public CartItem() {
    }

    public CartItem(String productId, String productName, double price, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CartItem{productId='" + productId + "', productName='" + productName
                + "', price=" + price + ", quantity=" + quantity + '}';
    }
}
