package com.tutorial.redis.module05.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 轉帳結果領域模型測試 — 驗證 TransferResult 的建構與參數驗證邏輯。
 * 此模型用於 MULTI/EXEC 交易轉帳操作的結果封裝，包含成功/失敗狀態與訊息。
 * 所屬層級：Domain 層（model），純領域物件的單元測試，不依賴任何 Redis 技術。
 */
@DisplayName("TransferResult 領域模型測試")
class TransferResultTest {

    // 驗證有效參數建構 TransferResult 時，所有欄位正確設定
    @Test
    @DisplayName("constructor_Valid — 有效參數建立成功且各欄位正確")
    void constructor_Valid() {
        // Act
        TransferResult result = new TransferResult("A", "B", 100.0, true, "Transfer successful");

        // Assert
        assertThat(result.getFromAccountId()).isEqualTo("A");
        assertThat(result.getToAccountId()).isEqualTo("B");
        assertThat(result.getAmount()).isEqualTo(100.0);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Transfer successful");
    }

    // 驗證金額為負數時，建構子拋出 IllegalArgumentException 防止無效資料
    @Test
    @DisplayName("constructor_NegativeAmount_ThrowsIAE — 金額為負數時拋出 IllegalArgumentException")
    void constructor_NegativeAmount_ThrowsIAE() {
        assertThatThrownBy(() -> new TransferResult("A", "B", -50.0, true, "Transfer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be positive");
    }
}
