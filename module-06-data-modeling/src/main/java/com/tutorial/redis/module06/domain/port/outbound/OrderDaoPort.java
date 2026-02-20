package com.tutorial.redis.module06.domain.port.outbound;

import com.tutorial.redis.module06.domain.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Order DAO operations.
 * Provides full CRUD plus secondary-index-based lookups.
 *
 * Secondary indexes:
 * - Set-based index by customerId for {@link #findByCustomerId(String)}
 * - Sorted Set time index (score = epoch millis) for {@link #findByCreatedAtRange(long, long)}
 */
public interface OrderDaoPort {

    void save(Order order);

    Optional<Order> findById(String orderId);

    void delete(String orderId);

    /**
     * Finds all orders for a given customer using a Set-based secondary index.
     */
    List<Order> findByCustomerId(String customerId);

    /**
     * Finds orders created within the given epoch-millisecond range
     * using a Sorted Set time index (ZRANGEBYSCORE).
     */
    List<Order> findByCreatedAtRange(long fromEpoch, long toEpoch);
}
