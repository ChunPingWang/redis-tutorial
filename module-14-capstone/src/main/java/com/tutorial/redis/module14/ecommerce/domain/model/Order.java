package com.tutorial.redis.module14.ecommerce.domain.model;

import java.util.List;

/**
 * Order aggregate.
 *
 * <p>Represents a customer order containing line items, total amount,
 * status, and creation timestamp. Published to Redis Streams for
 * event-driven order processing.</p>
 *
 * <p>The {@code items} field uses {@link List} of {@link CartItem} but
 * is stored as a simple string representation since the domain layer
 * has no Jackson dependency.</p>
 */
public class Order {

    private String orderId;
    private String customerId;
    private List<CartItem> items;
    private double totalAmount;
    private String status;
    private long createdAt;

    public Order() {
    }

    public Order(String orderId, String customerId, List<CartItem> items,
                 double totalAmount, String status, long createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Order{orderId='" + orderId + "', customerId='" + customerId
                + "', totalAmount=" + totalAmount + ", status='" + status
                + "', createdAt=" + createdAt + '}';
    }
}
