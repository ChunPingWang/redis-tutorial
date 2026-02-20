package com.tutorial.redis.module14.ecommerce.domain.port.inbound;

import com.tutorial.redis.module14.ecommerce.domain.model.Order;

import java.util.List;

/**
 * Inbound port for order operations.
 *
 * <p>Defines the primary use cases for creating orders and retrieving
 * recent orders for a customer via Redis Streams.</p>
 */
public interface OrderUseCase {

    void createOrder(Order order);

    List<Order> getRecentOrders(String customerId, int count);
}
