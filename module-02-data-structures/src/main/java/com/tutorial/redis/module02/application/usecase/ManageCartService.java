package com.tutorial.redis.module02.application.usecase;

import com.tutorial.redis.module02.domain.model.CartItem;
import com.tutorial.redis.module02.domain.model.ShoppingCart;
import com.tutorial.redis.module02.domain.port.inbound.ManageCartUseCase;
import com.tutorial.redis.module02.domain.port.outbound.ShoppingCartPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Application service implementing shopping cart management use cases.
 *
 * <p>Delegates to {@link ShoppingCartPort} for Redis Hash operations.
 * Demonstrates Redis HSET, HGET, HDEL, HGETALL for cart management.</p>
 */
@Service
public class ManageCartService implements ManageCartUseCase {

    private final ShoppingCartPort shoppingCartPort;

    public ManageCartService(ShoppingCartPort shoppingCartPort) {
        this.shoppingCartPort = shoppingCartPort;
    }

    @Override
    public void addToCart(String customerId, CartItem item) {
        shoppingCartPort.addItem(customerId, item);
    }

    @Override
    public void removeFromCart(String customerId, String productId) {
        shoppingCartPort.removeItem(customerId, productId);
    }

    @Override
    public void updateQuantity(String customerId, String productId, int newQuantity) {
        shoppingCartPort.updateItemQuantity(customerId, productId, newQuantity);
    }

    @Override
    public Optional<ShoppingCart> getCart(String customerId) {
        return shoppingCartPort.getCart(customerId);
    }

    @Override
    public void clearCart(String customerId) {
        shoppingCartPort.deleteCart(customerId);
    }
}
