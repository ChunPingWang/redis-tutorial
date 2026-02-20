package com.tutorial.redis.module02.domain.service;

import com.tutorial.redis.module02.domain.model.CartItem;
import com.tutorial.redis.module02.domain.model.ShoppingCart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CartService 領域服務測試")
class CartServiceTest {

    private final CartService cartService = new CartService();

    private CartItem createCartItem(String productId, String name, String price, int qty) {
        return new CartItem(productId, name, new BigDecimal(price), qty);
    }

    @Test
    @DisplayName("isValidQuantity_WhenInRange_ReturnsTrue — 數量 1~99 為有效")
    void isValidQuantity_WhenInRange_ReturnsTrue() {
        assertThat(cartService.isValidQuantity(1)).isTrue();
        assertThat(cartService.isValidQuantity(50)).isTrue();
        assertThat(cartService.isValidQuantity(99)).isTrue();
    }

    @Test
    @DisplayName("isValidQuantity_WhenZeroOrNegative_ReturnsFalse — 數量 <= 0 為無效")
    void isValidQuantity_WhenZeroOrNegative_ReturnsFalse() {
        assertThat(cartService.isValidQuantity(0)).isFalse();
        assertThat(cartService.isValidQuantity(-1)).isFalse();
        assertThat(cartService.isValidQuantity(-100)).isFalse();
    }

    @Test
    @DisplayName("isValidQuantity_WhenExceedsMax_ReturnsFalse — 數量超過 99 為無效")
    void isValidQuantity_WhenExceedsMax_ReturnsFalse() {
        assertThat(cartService.isValidQuantity(100)).isFalse();
        assertThat(cartService.isValidQuantity(999)).isFalse();
    }

    @Test
    @DisplayName("isCartFull_WhenAtMax_ReturnsTrue — 購物車達 50 項為已滿")
    void isCartFull_WhenAtMax_ReturnsTrue() {
        Map<String, CartItem> items = new LinkedHashMap<>();
        for (int i = 1; i <= 50; i++) {
            String id = "P-" + String.format("%03d", i);
            items.put(id, createCartItem(id, "Product " + i, "9.99", 1));
        }
        ShoppingCart fullCart = new ShoppingCart("CUST-001", items);

        assertThat(cartService.isCartFull(fullCart)).isTrue();
    }

    @Test
    @DisplayName("calculateCartTotal_WhenMultipleItems_ReturnsSumOfSubtotals — 計算購物車總金額")
    void calculateCartTotal_WhenMultipleItems_ReturnsSumOfSubtotals() {
        Map<String, CartItem> items = new LinkedHashMap<>();
        items.put("P-001", createCartItem("P-001", "Redis Book", "29.99", 2));   // 59.98
        items.put("P-002", createCartItem("P-002", "Spring Guide", "39.99", 1)); // 39.99
        items.put("P-003", createCartItem("P-003", "Docker Manual", "19.99", 3)); // 59.97
        ShoppingCart cart = new ShoppingCart("CUST-002", items);

        BigDecimal total = cartService.calculateCartTotal(cart);

        assertThat(total).isEqualByComparingTo(new BigDecimal("159.94"));
    }
}
