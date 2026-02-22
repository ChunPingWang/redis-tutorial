package com.tutorial.redis.module14.ecommerce.domain.service;

import com.tutorial.redis.module14.ecommerce.domain.model.CartItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * OrderTotalCalculator 領域服務單元測試類別。
 * 驗證訂單金額計算與折扣套用的純領域邏輯，不依賴 Redis。
 * 展示六角形架構中 domain 層的業務規則獨立測試。
 * 所屬：電商子系統 — domain 層
 */
@DisplayName("OrderTotalCalculator 單元測試")
class OrderTotalCalculatorTest {

    private final OrderTotalCalculator calculator = new OrderTotalCalculator();

    // 驗證計算多品項的金額總和（單價 x 數量的加總）
    @Test
    @DisplayName("calculateTotal_SumsAllItems — 計算所有品項金額總和")
    void calculateTotal_SumsAllItems() {
        // Arrange
        List<CartItem> items = List.of(
                new CartItem("p1", "Widget", 10.0, 2),
                new CartItem("p2", "Gadget", 25.50, 1),
                new CartItem("p3", "Doohickey", 5.0, 3)
        );

        // Act
        double total = calculator.calculateTotal(items);

        // Assert — (10*2) + (25.50*1) + (5*3) = 20 + 25.50 + 15 = 60.50
        assertThat(total).isCloseTo(60.50, within(0.001));
    }

    // 驗證套用百分比折扣後，金額正確減少
    @Test
    @DisplayName("applyDiscount_ReducesTotal — 套用折扣後金額應正確減少")
    void applyDiscount_ReducesTotal() {
        // Arrange
        double total = 100.0;
        double discountPercent = 15.0;

        // Act
        double discounted = calculator.applyDiscount(total, discountPercent);

        // Assert — 100 * (1 - 0.15) = 85.0
        assertThat(discounted).isCloseTo(85.0, within(0.001));
    }
}
