package com.tutorial.redis.module02.domain.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a customer's shopping cart.
 * Maps to Redis Hash structure (field = productId, value = CartItem).
 * Immutable value object — all fields are final.
 */
public class ShoppingCart {

    private final String customerId;
    private final Map<String, CartItem> items;

    public ShoppingCart(String customerId, Map<String, CartItem> items) {
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(items, "items must not be null");
        this.items = new LinkedHashMap<>(items);
    }

    public String getCustomerId() { return customerId; }

    /**
     * Returns an unmodifiable view of the cart items.
     */
    public Map<String, CartItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    /**
     * Retrieves a specific item from the cart by productId.
     */
    public Optional<CartItem> getItem(String productId) {
        return Optional.ofNullable(items.get(productId));
    }

    /**
     * Returns the number of distinct items in the cart.
     */
    public int itemCount() {
        return items.size();
    }

    /**
     * Calculates the total amount of all items in the cart.
     */
    public BigDecimal totalAmount() {
        return items.values().stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns true if the cart has no items.
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShoppingCart that)) return false;
        return customerId.equals(that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public String toString() {
        return "ShoppingCart{customerId='%s', itemCount=%d, total=%s}".formatted(
                customerId, itemCount(), totalAmount());
    }
}
