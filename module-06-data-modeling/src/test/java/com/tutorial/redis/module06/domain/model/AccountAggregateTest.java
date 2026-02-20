package com.tutorial.redis.module06.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AccountAggregate 領域模型測試")
class AccountAggregateTest {

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
