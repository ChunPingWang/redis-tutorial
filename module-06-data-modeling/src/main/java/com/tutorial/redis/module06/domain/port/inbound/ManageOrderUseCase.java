package com.tutorial.redis.module06.domain.port.inbound;

import com.tutorial.redis.module06.domain.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: manage orders using the DAO pattern with Redis.
 * Supports CRUD operations, secondary-index lookup by customer,
 * and time-range queries via Sorted Set index.
 */
public interface ManageOrderUseCase {

    void createOrder(Order order);

    Optional<Order> getOrder(String orderId);

    void deleteOrder(String orderId);

    /**
     * Finds all orders placed by a given customer.
     */
    List<Order> findOrdersByCustomer(String customerId);

    /**
     * Finds orders created within the given epoch-millisecond range.
     */
    List<Order> findOrdersByTimeRange(long fromEpoch, long toEpoch);
}
