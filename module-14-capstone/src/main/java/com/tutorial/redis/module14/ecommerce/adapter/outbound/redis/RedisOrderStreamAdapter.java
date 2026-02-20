package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.module14.ecommerce.domain.model.Order;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.OrderStreamPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis adapter for order stream operations.
 *
 * <p>Implements {@link OrderStreamPort} using Redis Streams. Orders are
 * published as stream entries with fields for orderId, customerId,
 * totalAmount, and status. Consumer groups are used for reliable
 * consumption of order events.</p>
 *
 * <p>Stream key: {@code ecommerce:orders}</p>
 */
@Component
public class RedisOrderStreamAdapter implements OrderStreamPort {

    private static final Logger log = LoggerFactory.getLogger(RedisOrderStreamAdapter.class);
    private static final String STREAM_KEY = "ecommerce:orders";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisOrderStreamAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void publishOrder(Order order) {
        log.debug("Publishing order {} to stream {}", order.getOrderId(), STREAM_KEY);
        Map<String, String> map = new HashMap<>();
        map.put("orderId", order.getOrderId());
        map.put("customerId", order.getCustomerId());
        map.put("totalAmount", String.valueOf(order.getTotalAmount()));
        map.put("status", order.getStatus());
        stringRedisTemplate.opsForStream().add(STREAM_KEY, map);
    }

    @Override
    public List<Map<String, String>> consumeOrders(String group, String consumer, int count) {
        log.debug("Consuming up to {} orders from group {} as consumer {}", count, group, consumer);

        // Create consumer group if needed, catching BUSYGROUP error
        ensureConsumerGroup(group);

        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                    .read(Consumer.from(group, consumer),
                            StreamReadOptions.empty().count(count),
                            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

            if (records == null || records.isEmpty()) {
                return Collections.emptyList();
            }

            List<Map<String, String>> result = new ArrayList<>();
            for (MapRecord<String, Object, Object> record : records) {
                Map<String, String> map = new HashMap<>();
                for (Map.Entry<Object, Object> entry : record.getValue().entrySet()) {
                    map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
                result.add(map);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to consume orders from stream: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Ensures the consumer group exists for the order stream.
     * Catches BUSYGROUP errors when the group already exists by walking
     * the exception cause chain.
     */
    private void ensureConsumerGroup(String group) {
        try {
            stringRedisTemplate.opsForStream()
                    .createGroup(STREAM_KEY, ReadOffset.from("0"), group);
            log.debug("Created consumer group {}", group);
        } catch (Exception e) {
            if (isBusyGroupError(e)) {
                log.debug("Consumer group {} already exists", group);
            } else {
                log.warn("Failed to create consumer group {}: {}", group, e.getMessage());
            }
        }
    }

    /**
     * Walks the exception cause chain to check for a BUSYGROUP error.
     */
    private boolean isBusyGroupError(Throwable t) {
        Throwable current = t;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
