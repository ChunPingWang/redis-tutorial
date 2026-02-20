package com.tutorial.redis.module01.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Product 領域模型測試")
class ProductTest {

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

    @Test
    @DisplayName("constructor_WhenNullProductId_ThrowsNPE")
    void constructor_WhenNullProductId_ThrowsNPE() {
        assertThatThrownBy(() -> new Product(null, "Laptop", BigDecimal.ONE, "Electronics", 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("productId");
    }

    @Test
    @DisplayName("constructor_WhenNegativeStock_ThrowsIAE")
    void constructor_WhenNegativeStock_ThrowsIAE() {
        assertThatThrownBy(() -> new Product("P-001", "Laptop", BigDecimal.ONE, "Electronics", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stockQuantity");
    }

    @Test
    @DisplayName("equals_WhenSameProductId_ReturnsTrue")
    void equals_WhenSameProductId_ReturnsTrue() {
        Product p1 = new Product("PROD-001", "A", BigDecimal.ONE, "X", 1);
        Product p2 = new Product("PROD-001", "B", BigDecimal.TEN, "Y", 2);

        assertThat(p1).isEqualTo(p2);
    }
}
