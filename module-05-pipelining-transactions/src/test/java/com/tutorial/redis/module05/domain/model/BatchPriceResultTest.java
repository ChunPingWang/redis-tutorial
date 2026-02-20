package com.tutorial.redis.module05.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BatchPriceResult 領域模型測試")
class BatchPriceResultTest {

    @Test
    @DisplayName("isFound_WhenPricePresent_ReturnsTrue — 價格存在時 isFound 回傳 true")
    void isFound_WhenPricePresent_ReturnsTrue() {
        // Arrange
        BatchPriceResult result = new BatchPriceResult("PROD-001", 99.99);

        // Assert
        assertThat(result.isFound()).isTrue();
        assertThat(result.getProductId()).isEqualTo("PROD-001");
        assertThat(result.getPrice()).isEqualTo(99.99);
    }

    @Test
    @DisplayName("isFound_WhenPriceNull_ReturnsFalse — 價格為 null 時 isFound 回傳 false")
    void isFound_WhenPriceNull_ReturnsFalse() {
        // Arrange
        BatchPriceResult result = new BatchPriceResult("MISSING-001", null);

        // Assert
        assertThat(result.isFound()).isFalse();
        assertThat(result.getProductId()).isEqualTo("MISSING-001");
        assertThat(result.getPrice()).isNull();
    }
}
