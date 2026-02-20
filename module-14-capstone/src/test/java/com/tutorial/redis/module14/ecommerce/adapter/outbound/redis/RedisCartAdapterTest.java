package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisCartAdapter 整合測試")
class RedisCartAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisCartAdapter adapter;

    @Test
    @DisplayName("addAndGetItems_ReturnsCartContents — 新增品項後取得購物車應包含所有品項")
    void addAndGetItems_ReturnsCartContents() {
        // Arrange
        String cartKey = "ecommerce:cart:test-customer";
        adapter.addItem(cartKey, "p1", "p1|Widget|10.0|2");
        adapter.addItem(cartKey, "p2", "p2|Gadget|25.5|1");

        // Act
        Map<String, String> items = adapter.getAllItems(cartKey);

        // Assert
        assertThat(items).hasSize(2);
        assertThat(items).containsKey("p1");
        assertThat(items).containsKey("p2");
        assertThat(items.get("p1")).isEqualTo("p1|Widget|10.0|2");
        assertThat(items.get("p2")).isEqualTo("p2|Gadget|25.5|1");
    }

    @Test
    @DisplayName("clearCart_RemovesAllItems — 清空購物車後應無品項")
    void clearCart_RemovesAllItems() {
        // Arrange
        String cartKey = "ecommerce:cart:test-customer";
        adapter.addItem(cartKey, "p1", "p1|Widget|10.0|2");
        adapter.addItem(cartKey, "p2", "p2|Gadget|25.5|1");

        // Act
        adapter.deleteCart(cartKey);

        // Assert
        Map<String, String> items = adapter.getAllItems(cartKey);
        assertThat(items).isEmpty();
    }
}
