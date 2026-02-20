package com.tutorial.redis.module06.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an order entity for the DAO pattern and secondary index demonstrations.
 *
 * Key schema: {@code order-service:order:{orderId}}
 * Secondary indexes:
 * - By customerId (Set): {@code order-service:order:index:customer:{customerId}}
 * - By createdAt (Sorted Set): {@code order-service:order:index:created_at}
 */
public class Order {

    private final String orderId;
    private final String customerId;
    private final double totalAmount;
    private final String status;
    private final List<OrderItem> items;
    private final Instant createdAt;

    public Order(String orderId, String customerId, double totalAmount, String status,
                 List<OrderItem> items, Instant createdAt) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.totalAmount = totalAmount;
        this.status = Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(items, "items must not be null");
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public List<OrderItem> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Returns the number of distinct line items in this order.
     */
    public int itemCount() {
        return items.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order that)) return false;
        return orderId.equals(that.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return "Order{orderId='%s', customerId='%s', totalAmount=%.2f, status='%s', items=%d}".formatted(
                orderId, customerId, totalAmount, status, items.size());
    }
}
