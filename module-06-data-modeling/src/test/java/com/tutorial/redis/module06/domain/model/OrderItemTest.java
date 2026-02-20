package com.tutorial.redis.module06.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderItem 領域模型測試")
class OrderItemTest {

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

    @Test
    @DisplayName("constructor_NegativeQuantity_ThrowsIAE — 數量為負數時拋出 IllegalArgumentException")
    void constructor_NegativeQuantity_ThrowsIAE() {
        assertThatThrownBy(() -> new OrderItem("prod-B", "Defective", -1, 10.00))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be greater than 0");
    }
}
