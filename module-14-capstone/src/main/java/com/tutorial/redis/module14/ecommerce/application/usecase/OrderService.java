package com.tutorial.redis.module14.ecommerce.application.usecase;

import com.tutorial.redis.module14.ecommerce.domain.model.Order;
import com.tutorial.redis.module14.ecommerce.domain.port.inbound.OrderUseCase;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.OrderStreamPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Application service implementing order use cases.
 *
 * <p>Publishes orders to a Redis Stream via the {@link OrderStreamPort}
 * and consumes recent orders from the stream for retrieval.</p>
 */
@Service
public class OrderService implements OrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String CONSUMER_GROUP = "order-processors";

    private final OrderStreamPort orderStreamPort;

    public OrderService(OrderStreamPort orderStreamPort) {
        this.orderStreamPort = orderStreamPort;
    }

    @Override
    public void createOrder(Order order) {
        log.info("Creating order {} for customer {}", order.getOrderId(), order.getCustomerId());
        orderStreamPort.publishOrder(order);
    }

    @Override
    public List<Order> getRecentOrders(String customerId, int count) {
        log.info("Retrieving {} recent orders for customer {}", count, customerId);
        String consumer = "consumer-" + customerId;
        List<Map<String, String>> records = orderStreamPort.consumeOrders(
                CONSUMER_GROUP, consumer, count);
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Order> orders = new ArrayList<>();
        for (Map<String, String> record : records) {
            Order order = mapToOrder(record);
            if (order != null) {
                orders.add(order);
            }
        }
        return orders;
    }

    /**
     * Maps a stream record map to an Order domain object.
     */
    private Order mapToOrder(Map<String, String> record) {
        try {
            Order order = new Order();
            order.setOrderId(record.getOrDefault("orderId", ""));
            order.setCustomerId(record.getOrDefault("customerId", ""));
            String totalStr = record.getOrDefault("totalAmount", "0");
            order.setTotalAmount(Double.parseDouble(totalStr));
            order.setStatus(record.getOrDefault("status", ""));
            return order;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse order from stream record: {}", record, e);
            return null;
        }
    }
}
