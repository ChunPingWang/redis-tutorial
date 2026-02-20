package com.tutorial.redis.module06.application.usecase;

import com.tutorial.redis.module06.domain.model.Order;
import com.tutorial.redis.module06.domain.port.inbound.ManageOrderUseCase;
import com.tutorial.redis.module06.domain.port.outbound.OrderDaoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service for managing orders using the DAO pattern with Redis.
 * Delegates all CRUD and index-based query operations to the
 * {@link OrderDaoPort} outbound port.
 */
@Service
public class ManageOrderService implements ManageOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageOrderService.class);

    private final OrderDaoPort orderDaoPort;

    public ManageOrderService(OrderDaoPort orderDaoPort) {
        this.orderDaoPort = orderDaoPort;
    }

    @Override
    public void createOrder(Order order) {
        log.debug("Creating order {}", order.getOrderId());
        orderDaoPort.save(order);
    }

    @Override
    public Optional<Order> getOrder(String orderId) {
        log.debug("Retrieving order {}", orderId);
        return orderDaoPort.findById(orderId);
    }

    @Override
    public void deleteOrder(String orderId) {
        log.debug("Deleting order {}", orderId);
        orderDaoPort.delete(orderId);
    }

    @Override
    public List<Order> findOrdersByCustomer(String customerId) {
        log.debug("Finding orders for customer {}", customerId);
        return orderDaoPort.findByCustomerId(customerId);
    }

    @Override
    public List<Order> findOrdersByTimeRange(long fromEpoch, long toEpoch) {
        log.debug("Finding orders in time range [{}, {}]", fromEpoch, toEpoch);
        return orderDaoPort.findByCreatedAtRange(fromEpoch, toEpoch);
    }
}
