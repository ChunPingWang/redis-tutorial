package com.tutorial.redis.module02.domain.port.inbound;

import com.tutorial.redis.module02.domain.model.CartItem;
import com.tutorial.redis.module02.domain.model.ShoppingCart;

import java.util.Optional;

/**
 * Inbound port: manage shopping carts using Redis Hash structure.
 */
public interface ManageCartUseCase {

    void addToCart(String customerId, CartItem item);

    void removeFromCart(String customerId, String productId);

    void updateQuantity(String customerId, String productId, int newQuantity);

    Optional<ShoppingCart> getCart(String customerId);

    void clearCart(String customerId);
}
