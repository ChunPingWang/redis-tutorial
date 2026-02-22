package com.tutorial.redis.module02.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 購物車項目領域模型測試
 * 驗證 CartItem 的建構子驗證邏輯與小計金額計算。
 * 對應 Redis Hash 中單一 field 的值物件。
 * 層級：Domain（領域模型）
 */
@DisplayName("CartItem 領域模型測試")
class CartItemTest {

    // 驗證使用合法參數建構 CartItem 後各欄位值正確
    @Test
    @DisplayName("constructor_WhenValidArgs_CreatesCartItem — 建立有效的購物車項目")
    void constructor_WhenValidArgs_CreatesCartItem() {
        CartItem item = new CartItem("P-001", "Redis Book", new BigDecimal("29.99"), 2);

        assertThat(item.getProductId()).isEqualTo("P-001");
        assertThat(item.getProductName()).isEqualTo("Redis Book");
        assertThat(item.getUnitPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    // 驗證 productId 為 null 時拋出 NullPointerException
    @Test
    @DisplayName("constructor_WhenNullProductId_ThrowsNPE — null productId 拋出 NullPointerException")
    void constructor_WhenNullProductId_ThrowsNPE() {
        assertThatThrownBy(() -> new CartItem(null, "Redis Book", new BigDecimal("29.99"), 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("productId");
    }

    // 驗證數量為 0 時拋出 IllegalArgumentException
    @Test
    @DisplayName("constructor_WhenZeroQuantity_ThrowsIAE — 數量為 0 拋出 IllegalArgumentException")
    void constructor_WhenZeroQuantity_ThrowsIAE() {
        assertThatThrownBy(() -> new CartItem("P-001", "Redis Book", new BigDecimal("29.99"), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");
    }

    // 驗證小計金額計算（單價 * 數量）結果正確
    @Test
    @DisplayName("subtotal_ReturnsCorrectValue — 29.99 * 3 = 89.97")
    void subtotal_ReturnsCorrectValue() {
        CartItem item = new CartItem("P-001", "Redis Book", new BigDecimal("29.99"), 3);

        BigDecimal subtotal = item.subtotal();

        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("89.97"));
    }
}
