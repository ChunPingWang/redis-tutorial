package com.tutorial.redis.module14.ecommerce.domain.port.inbound;

import com.tutorial.redis.module14.ecommerce.domain.model.CartItem;

import java.util.List;

/**
 * Inbound port for shopping cart operations.
 *
 * <p>Defines the primary use cases for managing a customer's shopping cart,
 * including adding items, retrieving the full cart, and clearing it.</p>
 */
public interface CartUseCase {

    void addToCart(String customerId, CartItem item);

    List<CartItem> getCart(String customerId);

    void clearCart(String customerId);
}
