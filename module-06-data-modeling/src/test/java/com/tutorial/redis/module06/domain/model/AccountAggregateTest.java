package com.tutorial.redis.module06.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 測試 AccountAggregate 領域模型的建構與驗證邏輯。
 * 驗證帳戶聚合根的欄位初始化與 null 參數防禦，確保領域不變式（invariant）。
 * 屬於 Domain 層（領域模型），與 Redis 資料建模無直接耦合。
 */
@DisplayName("AccountAggregate 領域模型測試")
class AccountAggregateTest {

    // 驗證以有效參數建構帳戶聚合根，所有欄位正確初始化
    @Test
    @DisplayName("constructor_Valid — 有效參數建立成功且各欄位正確")
    void constructor_Valid() {
        // Arrange
        Instant createdAt = Instant.parse("2024-06-15T10:00:00Z");

        // Act
        AccountAggregate account = new AccountAggregate(
                "acct-001", "Alice Wang", 5000.50, "USD", createdAt, "ACTIVE"
        );

        // Assert
        assertThat(account.getAccountId()).isEqualTo("acct-001");
        assertThat(account.getHolderName()).isEqualTo("Alice Wang");
        assertThat(account.getBalance()).isEqualTo(5000.50);
        assertThat(account.getCurrency()).isEqualTo("USD");
        assertThat(account.getCreatedAt()).isEqualTo(createdAt);
        assertThat(account.getStatus()).isEqualTo("ACTIVE");
    }

    // 驗證 accountId 為 null 時拋出 NullPointerException，確保領域不變式
    @Test
    @DisplayName("constructor_NullAccountId_ThrowsNPE — accountId 為 null 時拋出 NullPointerException")
    void constructor_NullAccountId_ThrowsNPE() {
        assertThatThrownBy(() -> new AccountAggregate(
                null, "Alice Wang", 5000.50, "USD", Instant.now(), "ACTIVE"
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("accountId must not be null");
    }
}
