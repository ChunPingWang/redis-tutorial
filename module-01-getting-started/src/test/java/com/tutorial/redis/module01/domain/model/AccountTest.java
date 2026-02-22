package com.tutorial.redis.module01.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Account 領域模型單元測試
 * 驗證帳戶物件的建構、欄位初始化、空值防護及以 accountId 為基準的相等性判斷。
 * 此模型作為 Redis 快取的核心資料結構。
 * 層級：Domain（領域模型層）
 */
@DisplayName("Account 領域模型測試")
class AccountTest {

    // 驗證以合法參數建構帳戶時，所有欄位正確初始化
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

    // 驗證 accountId 為 null 時拋出 NullPointerException
    @Test
    @DisplayName("constructor_WhenNullAccountId_ThrowsNPE")
    void constructor_WhenNullAccountId_ThrowsNPE() {
        assertThatThrownBy(() -> new Account(null, "Alice", BigDecimal.ONE, "USD", Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("accountId");
    }

    // 驗證相同 accountId 的兩個帳戶視為相等
    @Test
    @DisplayName("equals_WhenSameAccountId_ReturnsTrue")
    void equals_WhenSameAccountId_ReturnsTrue() {
        Account a1 = new Account("ACC-001", "Alice", BigDecimal.ONE, "USD", Instant.now());
        Account a2 = new Account("ACC-001", "Bob", BigDecimal.TEN, "EUR", Instant.now());

        assertThat(a1).isEqualTo(a2);
    }

    // 驗證不同 accountId 的兩個帳戶視為不相等
    @Test
    @DisplayName("equals_WhenDifferentAccountId_ReturnsFalse")
    void equals_WhenDifferentAccountId_ReturnsFalse() {
        Account a1 = new Account("ACC-001", "Alice", BigDecimal.ONE, "USD", Instant.now());
        Account a2 = new Account("ACC-002", "Alice", BigDecimal.ONE, "USD", Instant.now());

        assertThat(a1).isNotEqualTo(a2);
    }
}
