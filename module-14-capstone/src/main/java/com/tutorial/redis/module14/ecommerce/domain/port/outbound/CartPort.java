package com.tutorial.redis.module14.ecommerce.domain.port.outbound;

import java.util.Map;

/**
 * Outbound port for shopping cart persistence.
 *
 * <p>Abstracts Redis Hash operations for storing and retrieving cart items.
 * Each cart item is stored as a hash field keyed by product ID with a
 * JSON string value.</p>
 */
public interface CartPort {

    void addItem(String cartKey, String productId, String itemJson);

    Map<String, String> getAllItems(String cartKey);

    void deleteCart(String cartKey);
}
