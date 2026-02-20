package com.tutorial.redis.module06.adapter.outbound.redis;

import com.tutorial.redis.module06.domain.model.Order;
import com.tutorial.redis.module06.domain.port.outbound.OrderDaoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Redis adapter implementing the Order DAO using the JSON String pattern.
 * Each order is stored as a single Redis String whose value is the Jackson-serialized
 * JSON of the {@link Order} object (via the Jackson-configured {@code RedisTemplate}).
 *
 * <p>Secondary indexes are maintained separately using {@code StringRedisTemplate}:</p>
 * <ul>
 *   <li>Customer index (SET): enables lookup by customerId</li>
 *   <li>Time index (SORTED SET): enables range queries by createdAt epoch millis</li>
 * </ul>
 *
 * <h3>Key Schema</h3>
 * <ul>
 *   <li>Entity: {@code ecommerce:order:{orderId}} (STRING with JSON value)</li>
 *   <li>Customer index: {@code idx:order:customer:{customerId}} (SET of orderIds)</li>
 *   <li>Time index: {@code idx:order:created} (ZSET, score=createdAt epoch millis, member=orderId)</li>
 * </ul>
 */
@Component
public class RedisOrderDaoAdapter implements OrderDaoPort {

    private static final Logger log = LoggerFactory.getLogger(RedisOrderDaoAdapter.class);

    private static final String ENTITY_PREFIX = "ecommerce:order:";
    private static final String CUSTOMER_INDEX_PREFIX = "idx:order:customer:";
    private static final String TIME_INDEX_KEY = "idx:order:created";

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisOrderDaoAdapter(RedisTemplate<String, Object> redisTemplate,
                                StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Saves an order as a JSON String in Redis and adds the orderId to the customer
     * Set index and the time-based Sorted Set index.
     * Uses {@code RedisTemplate<String, Object>} for the entity (JSON serialization)
     * and {@code StringRedisTemplate} for the secondary indexes.
     */
    @Override
    public void save(Order order) {
        String key = ENTITY_PREFIX + order.getOrderId();

        // Store order as JSON String via Jackson-configured RedisTemplate
        redisTemplate.opsForValue().set(key, order);

        // Add to customer index (SET)
        String customerIndexKey = CUSTOMER_INDEX_PREFIX + order.getCustomerId();
        stringRedisTemplate.opsForSet().add(customerIndexKey, order.getOrderId());

        // Add to time index (SORTED SET with createdAt as score)
        stringRedisTemplate.opsForZSet().add(
                TIME_INDEX_KEY,
                order.getOrderId(),
                order.getCreatedAt().toEpochMilli()
        );

        log.debug("Saved order {} with customer index [{}] and time index score [{}]",
                order.getOrderId(), customerIndexKey, order.getCreatedAt().toEpochMilli());
    }

    /**
     * Retrieves an order by performing a GET on the entity key and deserializing
     * the JSON value back into an {@link Order} object.
     */
    @Override
    public Optional<Order> findById(String orderId) {
        String key = ENTITY_PREFIX + orderId;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            log.debug("Order {} not found", orderId);
            return Optional.empty();
        }

        Order order = (Order) value;
        log.debug("Found order {}", orderId);
        return Optional.of(order);
    }

    /**
     * Deletes an order by first reading it to determine the customerId (for index
     * cleanup), then removing the orderId from both secondary indexes, and finally
     * deleting the entity key.
     */
    @Override
    public void delete(String orderId) {
        String key = ENTITY_PREFIX + orderId;

        // Read order first to determine customer index key for cleanup
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            Order order = (Order) value;
            String customerIndexKey = CUSTOMER_INDEX_PREFIX + order.getCustomerId();
            stringRedisTemplate.opsForSet().remove(customerIndexKey, orderId);
        }

        // Remove from time index
        stringRedisTemplate.opsForZSet().remove(TIME_INDEX_KEY, orderId);

        // Delete entity
        redisTemplate.delete(key);
        log.debug("Deleted order {}", orderId);
    }

    /**
     * Finds all orders for a given customer by reading the
     * {@code idx:order:customer:{customerId}} Set, then fetching each order
     * by its ID.
     */
    @Override
    public List<Order> findByCustomerId(String customerId) {
        String indexKey = CUSTOMER_INDEX_PREFIX + customerId;
        Set<String> orderIds = stringRedisTemplate.opsForSet().members(indexKey);

        if (orderIds == null || orderIds.isEmpty()) {
            log.debug("No orders found for customer {}", customerId);
            return List.of();
        }

        List<Order> orders = new ArrayList<>();
        for (String orderId : orderIds) {
            findById(orderId).ifPresent(orders::add);
        }

        log.debug("Found {} orders for customer {}", orders.size(), customerId);
        return orders;
    }

    /**
     * Finds orders created within the given epoch-millisecond range by querying
     * the {@code idx:order:created} Sorted Set with {@code ZRANGEBYSCORE},
     * then fetching each order by its ID.
     */
    @Override
    public List<Order> findByCreatedAtRange(long fromEpoch, long toEpoch) {
        Set<String> orderIds = stringRedisTemplate.opsForZSet().rangeByScore(
                TIME_INDEX_KEY, fromEpoch, toEpoch
        );

        if (orderIds == null || orderIds.isEmpty()) {
            log.debug("No orders found in time range [{}, {}]", fromEpoch, toEpoch);
            return List.of();
        }

        List<Order> orders = new ArrayList<>();
        for (String orderId : orderIds) {
            findById(orderId).ifPresent(orders::add);
        }

        log.debug("Found {} orders in time range [{}, {}]", orders.size(), fromEpoch, toEpoch);
        return orders;
    }
}
