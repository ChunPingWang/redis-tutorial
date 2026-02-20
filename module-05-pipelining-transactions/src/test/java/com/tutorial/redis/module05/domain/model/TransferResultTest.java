package com.tutorial.redis.module05.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransferResult 領域模型測試")
class TransferResultTest {

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

    @Test
    @DisplayName("constructor_NegativeAmount_ThrowsIAE — 金額為負數時拋出 IllegalArgumentException")
    void constructor_NegativeAmount_ThrowsIAE() {
        assertThatThrownBy(() -> new TransferResult("A", "B", -50.0, true, "Transfer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be positive");
    }
}
