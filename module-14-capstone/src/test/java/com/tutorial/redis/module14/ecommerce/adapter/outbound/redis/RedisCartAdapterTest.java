package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisCartAdapter 整合測試類別。
 * 驗證使用 Redis Hash 實作購物車的新增品項、取得品項與清空購物車功能。
 * 展示 Redis Hash（HSET/HGETALL/DEL）在電商購物車場景的應用。
 * 所屬：電商子系統 — adapter 層
 */
@DisplayName("RedisCartAdapter 整合測試")
class RedisCartAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisCartAdapter adapter;

    // 驗證新增多個品項後，取得購物車應回傳所有已加入的品項
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

    // 驗證清空購物車後，再次取得應回傳空的 Map
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
