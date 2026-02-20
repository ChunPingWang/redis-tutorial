package com.tutorial.redis.module14.ecommerce.domain.port.outbound;

import com.tutorial.redis.module14.ecommerce.domain.model.Order;

import java.util.List;
import java.util.Map;

/**
 * Outbound port for order stream operations.
 *
 * <p>Abstracts Redis Stream operations for publishing orders as stream
 * entries and consuming them via consumer groups for event-driven
 * processing.</p>
 */
public interface OrderStreamPort {

    void publishOrder(Order order);

    List<Map<String, String>> consumeOrders(String group, String consumer, int count);
}
