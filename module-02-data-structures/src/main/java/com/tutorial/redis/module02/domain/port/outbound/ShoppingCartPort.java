package com.tutorial.redis.module02.domain.port.outbound;

import com.tutorial.redis.module02.domain.model.CartItem;
import com.tutorial.redis.module02.domain.model.ShoppingCart;

import java.util.Optional;

/**
 * Outbound port for shopping cart operations.
 * Uses Redis Hash structure (field = productId, value = serialized CartItem).
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface ShoppingCartPort {

    void addItem(String customerId, CartItem item);

    void removeItem(String customerId, String productId);

    void updateItemQuantity(String customerId, String productId, int quantity);

    Optional<CartItem> getItem(String customerId, String productId);

    Optional<ShoppingCart> getCart(String customerId);

    boolean cartExists(String customerId);

    void deleteCart(String customerId);
}
