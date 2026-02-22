package com.tutorial.redis.module01.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Product 領域模型單元測試
 * 驗證商品物件的建構、欄位初始化、空值防護、庫存負數防護及相等性判斷。
 * 此模型作為 Redis 快取的核心資料結構。
 * 層級：Domain（領域模型層）
 */
@DisplayName("Product 領域模型測試")
class ProductTest {

    // 驗證以合法參數建構商品時，所有欄位正確初始化
    @Test
    @DisplayName("constructor_WhenValidArgs_CreatesProduct")
    void constructor_WhenValidArgs_CreatesProduct() {
        Product product = new Product("PROD-001", "Laptop", new BigDecimal("999.99"), "Electronics", 50);

        assertThat(product.getProductId()).isEqualTo("PROD-001");
        assertThat(product.getName()).isEqualTo("Laptop");
        assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(product.getCategory()).isEqualTo("Electronics");
        assertThat(product.getStockQuantity()).isEqualTo(50);
    }

    // 驗證 productId 為 null 時拋出 NullPointerException
    @Test
    @DisplayName("constructor_WhenNullProductId_ThrowsNPE")
    void constructor_WhenNullProductId_ThrowsNPE() {
        assertThatThrownBy(() -> new Product(null, "Laptop", BigDecimal.ONE, "Electronics", 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("productId");
    }

    // 驗證庫存數量為負數時拋出 IllegalArgumentException
    @Test
    @DisplayName("constructor_WhenNegativeStock_ThrowsIAE")
    void constructor_WhenNegativeStock_ThrowsIAE() {
        assertThatThrownBy(() -> new Product("P-001", "Laptop", BigDecimal.ONE, "Electronics", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stockQuantity");
    }

    // 驗證相同 productId 的兩個商品視為相等
    @Test
    @DisplayName("equals_WhenSameProductId_ReturnsTrue")
    void equals_WhenSameProductId_ReturnsTrue() {
        Product p1 = new Product("PROD-001", "A", BigDecimal.ONE, "X", 1);
        Product p2 = new Product("PROD-001", "B", BigDecimal.TEN, "Y", 2);

        assertThat(p1).isEqualTo(p2);
    }
}
