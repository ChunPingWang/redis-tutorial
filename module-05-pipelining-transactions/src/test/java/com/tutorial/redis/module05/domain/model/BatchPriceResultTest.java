package com.tutorial.redis.module05.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 批次價格結果領域模型測試 — 驗證 BatchPriceResult 的價格存在判斷邏輯。
 * 此模型用於 Pipeline 批次查詢商品價格的回傳結果封裝。
 * 所屬層級：Domain 層（model），純領域物件的單元測試，不依賴任何 Redis 技術。
 */
@DisplayName("BatchPriceResult 領域模型測試")
class BatchPriceResultTest {

    // 驗證價格有值時，isFound() 回傳 true 且能正確取得商品 ID 與價格
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

    // 驗證價格為 null 時，isFound() 回傳 false 表示該商品價格不存在
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
