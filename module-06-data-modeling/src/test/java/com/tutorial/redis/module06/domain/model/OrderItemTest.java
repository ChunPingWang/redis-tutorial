package com.tutorial.redis.module06.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 測試 OrderItem 領域模型的小計計算與參數驗證。
 * 驗證訂單品項的 subtotal 計算邏輯及數量負數的防禦檢查。
 * 屬於 Domain 層（領域模型），OrderItem 作為 Order 的內嵌值物件（embedded value object）。
 */
@DisplayName("OrderItem 領域模型測試")
class OrderItemTest {

    // 驗證 subtotal() 正確計算數量乘以單價的小計金額
    @Test
    @DisplayName("subtotal_ReturnsCorrectValue — 數量 3、單價 10 的小計應為 30")
    void subtotal_ReturnsCorrectValue() {
        // Arrange
        OrderItem item = new OrderItem("prod-A", "Widget", 3, 10.00);

        // Act
        double subtotal = item.subtotal();

        // Assert
        assertThat(subtotal).isEqualTo(30.00);
    }

    // 驗證數量為負數時拋出 IllegalArgumentException，防止無效品項
    @Test
    @DisplayName("constructor_NegativeQuantity_ThrowsIAE — 數量為負數時拋出 IllegalArgumentException")
    void constructor_NegativeQuantity_ThrowsIAE() {
        assertThatThrownBy(() -> new OrderItem("prod-B", "Defective", -1, 10.00))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be greater than 0");
    }
}
