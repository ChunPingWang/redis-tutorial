package com.tutorial.redis.module06.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order 領域模型測試")
class OrderTest {

    @Test
    @DisplayName("constructor_Valid_WithItems — 有效參數建立含品項的訂單，各欄位正確")
    void constructor_Valid_WithItems() {
        // Arrange
        List<OrderItem> items = List.of(
                new OrderItem("prod-A", "Keyboard", 2, 49.99),
                new OrderItem("prod-B", "Mouse", 1, 29.99)
        );
        Instant createdAt = Instant.parse("2024-06-15T12:00:00Z");

        // Act
        Order order = new Order("ord-001", "cust-A", 129.97, "PENDING", items, createdAt);

        // Assert
        assertThat(order.getOrderId()).isEqualTo("ord-001");
        assertThat(order.getCustomerId()).isEqualTo("cust-A");
        assertThat(order.getTotalAmount()).isEqualTo(129.97);
        assertThat(order.getStatus()).isEqualTo("PENDING");
        assertThat(order.getCreatedAt()).isEqualTo(createdAt);
        assertThat(order.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("itemCount_ReturnsCorrectCount — itemCount 應回傳正確的品項數量")
    void itemCount_ReturnsCorrectCount() {
        // Arrange
        List<OrderItem> items = List.of(
                new OrderItem("prod-A", "Keyboard", 2, 49.99),
                new OrderItem("prod-B", "Mouse", 1, 29.99),
                new OrderItem("prod-C", "Monitor", 1, 399.99)
        );
        Order order = new Order("ord-002", "cust-B", 529.96, "PENDING", items, Instant.now());

        // Act & Assert
        assertThat(order.itemCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("items_AreUnmodifiable — getItems 回傳的列表不可修改，嘗試新增應拋出 UnsupportedOperationException")
    void items_AreUnmodifiable() {
        // Arrange
        List<OrderItem> items = List.of(
                new OrderItem("prod-A", "Keyboard", 1, 49.99)
        );
        Order order = new Order("ord-003", "cust-C", 49.99, "PENDING", items, Instant.now());

        // Act & Assert
        List<OrderItem> retrievedItems = order.getItems();
        assertThatThrownBy(() -> retrievedItems.add(
                new OrderItem("prod-X", "Hacker Item", 1, 0.01)
        ))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
