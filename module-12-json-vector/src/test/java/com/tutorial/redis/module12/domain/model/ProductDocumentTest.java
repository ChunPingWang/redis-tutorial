package com.tutorial.redis.module12.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductDocument 領域模型測試")
class ProductDocumentTest {

    @Test
    @DisplayName("constructor_WithVariants_ReturnsCorrectData — 含變體的商品文件應正確建立")
    void constructor_WithVariants_ReturnsCorrectData() {
        // Arrange — create 2 variants and a review summary
        ProductVariant variant1 = new ProductVariant("SKU-001", "Red", 149.99, 50);
        ProductVariant variant2 = new ProductVariant("SKU-002", "Blue", 159.99, 30);
        ReviewSummary reviews = new ReviewSummary(4.5, 120);

        // Act — create a product document with all fields
        ProductDocument product = new ProductDocument(
                "P001", "Wireless Headphones", "High quality audio",
                149.99, "electronics",
                List.of(variant1, variant2), reviews
        );

        // Assert — verify all fields are correctly set
        assertThat(product.getProductId()).isEqualTo("P001");
        assertThat(product.getName()).isEqualTo("Wireless Headphones");
        assertThat(product.getDescription()).isEqualTo("High quality audio");
        assertThat(product.getPrice()).isEqualTo(149.99);
        assertThat(product.getCategory()).isEqualTo("electronics");
        assertThat(product.getVariants()).hasSize(2);
        assertThat(product.getVariants().get(0).getSku()).isEqualTo("SKU-001");
        assertThat(product.getVariants().get(0).getColor()).isEqualTo("Red");
        assertThat(product.getVariants().get(0).getPrice()).isEqualTo(149.99);
        assertThat(product.getVariants().get(0).getStock()).isEqualTo(50);
        assertThat(product.getVariants().get(1).getSku()).isEqualTo("SKU-002");
        assertThat(product.getVariants().get(1).getColor()).isEqualTo("Blue");
        assertThat(product.getReviews()).isNotNull();
        assertThat(product.getReviews().getAverageRating()).isEqualTo(4.5);
        assertThat(product.getReviews().getCount()).isEqualTo(120);
    }

    @Test
    @DisplayName("variants_AreAccessible — 變體列表應可存取")
    void variants_AreAccessible() {
        // Arrange — create a product with no-arg constructor and set variants
        ProductDocument product = new ProductDocument();
        ProductVariant variant = new ProductVariant("SKU-100", "Green", 99.99, 10);
        product.setVariants(List.of(variant));

        // Act & Assert — variants should be accessible and correct
        assertThat(product.getVariants()).isNotNull();
        assertThat(product.getVariants()).hasSize(1);
        assertThat(product.getVariants().get(0).getSku()).isEqualTo("SKU-100");
        assertThat(product.getVariants().get(0).getColor()).isEqualTo("Green");
        assertThat(product.getVariants().get(0).getPrice()).isEqualTo(99.99);
        assertThat(product.getVariants().get(0).getStock()).isEqualTo(10);
    }
}
