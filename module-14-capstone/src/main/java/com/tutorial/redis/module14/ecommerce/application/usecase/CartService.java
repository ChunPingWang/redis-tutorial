package com.tutorial.redis.module14.ecommerce.application.usecase;

import com.tutorial.redis.module14.ecommerce.domain.model.CartItem;
import com.tutorial.redis.module14.ecommerce.domain.port.inbound.CartUseCase;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.CartPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Application service implementing shopping cart use cases.
 *
 * <p>Serializes {@link CartItem} instances to a simple string format for
 * storage via the {@link CartPort}, and deserializes them back when
 * retrieving the cart contents.</p>
 */
@Service
public class CartService implements CartUseCase {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final String CART_KEY_PREFIX = "ecommerce:cart:";

    private final CartPort cartPort;

    public CartService(CartPort cartPort) {
        this.cartPort = cartPort;
    }

    @Override
    public void addToCart(String customerId, CartItem item) {
        log.info("Adding item {} to cart for customer {}", item.getProductId(), customerId);
        String cartKey = CART_KEY_PREFIX + customerId;
        String itemJson = serializeCartItem(item);
        cartPort.addItem(cartKey, item.getProductId(), itemJson);
    }

    @Override
    public List<CartItem> getCart(String customerId) {
        log.info("Retrieving cart for customer {}", customerId);
        String cartKey = CART_KEY_PREFIX + customerId;
        Map<String, String> items = cartPort.getAllItems(cartKey);
        List<CartItem> cartItems = new ArrayList<>();
        for (String json : items.values()) {
            CartItem item = deserializeCartItem(json);
            if (item != null) {
                cartItems.add(item);
            }
        }
        return cartItems;
    }

    @Override
    public void clearCart(String customerId) {
        log.info("Clearing cart for customer {}", customerId);
        String cartKey = CART_KEY_PREFIX + customerId;
        cartPort.deleteCart(cartKey);
    }

    /**
     * Serializes a CartItem to a simple delimited string.
     * Format: {@code productId|productName|price|quantity}
     */
    private String serializeCartItem(CartItem item) {
        return item.getProductId() + "|" + item.getProductName() + "|"
                + item.getPrice() + "|" + item.getQuantity();
    }

    /**
     * Deserializes a CartItem from a delimited string.
     */
    private CartItem deserializeCartItem(String json) {
        String[] parts = json.split("\\|");
        if (parts.length < 4) {
            log.warn("Invalid cart item format: {}", json);
            return null;
        }
        try {
            return new CartItem(parts[0], parts[1],
                    Double.parseDouble(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse cart item: {}", json, e);
            return null;
        }
    }
}
