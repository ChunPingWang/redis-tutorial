package com.tutorial.redis.module01.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account 領域模型測試")
class AccountTest {

    @Test
    @DisplayName("constructor_WhenValidArgs_CreatesAccount")
    void constructor_WhenValidArgs_CreatesAccount() {
        Instant now = Instant.now();
        Account account = new Account("ACC-001", "Alice", new BigDecimal("1000.00"), "USD", now);

        assertThat(account.getAccountId()).isEqualTo("ACC-001");
        assertThat(account.getHolderName()).isEqualTo("Alice");
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(account.getCurrency()).isEqualTo("USD");
        assertThat(account.getLastUpdated()).isEqualTo(now);
    }

    @Test
    @DisplayName("constructor_WhenNullAccountId_ThrowsNPE")
    void constructor_WhenNullAccountId_ThrowsNPE() {
        assertThatThrownBy(() -> new Account(null, "Alice", BigDecimal.ONE, "USD", Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("accountId");
    }

    @Test
    @DisplayName("equals_WhenSameAccountId_ReturnsTrue")
    void equals_WhenSameAccountId_ReturnsTrue() {
        Account a1 = new Account("ACC-001", "Alice", BigDecimal.ONE, "USD", Instant.now());
        Account a2 = new Account("ACC-001", "Bob", BigDecimal.TEN, "EUR", Instant.now());

        assertThat(a1).isEqualTo(a2);
    }

    @Test
    @DisplayName("equals_WhenDifferentAccountId_ReturnsFalse")
    void equals_WhenDifferentAccountId_ReturnsFalse() {
        Account a1 = new Account("ACC-001", "Alice", BigDecimal.ONE, "USD", Instant.now());
        Account a2 = new Account("ACC-002", "Alice", BigDecimal.ONE, "USD", Instant.now());

        assertThat(a1).isNotEqualTo(a2);
    }
}
