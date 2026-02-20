package com.tutorial.redis.module02.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShoppingCart 領域模型測試")
class ShoppingCartTest {

    private CartItem createCartItem(String productId, String name, String price, int qty) {
        return new CartItem(productId, name, new BigDecimal(price), qty);
    }

    @Test
    @DisplayName("constructor_WhenValidArgs_CreatesCart — 建立有效的購物車")
    void constructor_WhenValidArgs_CreatesCart() {
        Map<String, CartItem> items = new LinkedHashMap<>();
        items.put("P-001", createCartItem("P-001", "Redis Book", "29.99", 1));

        ShoppingCart cart = new ShoppingCart("CUST-001", items);

        assertThat(cart.getCustomerId()).isEqualTo("CUST-001");
        assertThat(cart.itemCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getItem_WhenExists_ReturnsItem — 取得存在的商品")
    void getItem_WhenExists_ReturnsItem() {
        Map<String, CartItem> items = new LinkedHashMap<>();
        CartItem item = createCartItem("P-001", "Redis Book", "29.99", 2);
        items.put("P-001", item);

        ShoppingCart cart = new ShoppingCart("CUST-002", items);

        assertThat(cart.getItem("P-001")).isPresent();
        assertThat(cart.getItem("P-001").get().getProductName()).isEqualTo("Redis Book");
        assertThat(cart.getItem("P-999")).isEmpty();
    }

    @Test
    @DisplayName("totalAmount_WhenMultipleItems_ReturnsSumOfSubtotals — 計算購物車總金額")
    void totalAmount_WhenMultipleItems_ReturnsSumOfSubtotals() {
        Map<String, CartItem> items = new LinkedHashMap<>();
        items.put("P-001", createCartItem("P-001", "Redis Book", "29.99", 2));   // 59.98
        items.put("P-002", createCartItem("P-002", "Spring Guide", "39.99", 1)); // 39.99

        ShoppingCart cart = new ShoppingCart("CUST-003", items);

        assertThat(cart.totalAmount()).isEqualByComparingTo(new BigDecimal("99.97"));
    }

    @Test
    @DisplayName("isEmpty_WhenNoItems_ReturnsTrue — 空購物車回傳 true")
    void isEmpty_WhenNoItems_ReturnsTrue() {
        ShoppingCart cart = new ShoppingCart("CUST-004", new LinkedHashMap<>());

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.itemCount()).isEqualTo(0);
    }
}
