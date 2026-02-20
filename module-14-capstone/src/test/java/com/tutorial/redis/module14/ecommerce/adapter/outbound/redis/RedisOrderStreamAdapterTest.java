package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module14.ecommerce.domain.model.CartItem;
import com.tutorial.redis.module14.ecommerce.domain.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisOrderStreamAdapter 整合測試")
class RedisOrderStreamAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisOrderStreamAdapter adapter;

    @Test
    @DisplayName("publishAndConsume_ReturnsOrderData — 發布訂單後消費應取得訂單資料")
    void publishAndConsume_ReturnsOrderData() {
        // Arrange — publish an order
        Order order = new Order("order-1", "customer-1",
                List.of(new CartItem("p1", "Widget", 10.0, 2)),
                20.0, "PENDING", System.currentTimeMillis());
        adapter.publishOrder(order);

        // Act — consume the order
        List<Map<String, String>> consumed = adapter.consumeOrders(
                "test-group", "test-consumer", 10);

        // Assert
        assertThat(consumed).hasSize(1);
        Map<String, String> record = consumed.get(0);
        assertThat(record.get("orderId")).isEqualTo("order-1");
        assertThat(record.get("customerId")).isEqualTo("customer-1");
        assertThat(record.get("totalAmount")).isEqualTo("20.0");
        assertThat(record.get("status")).isEqualTo("PENDING");
    }
}
