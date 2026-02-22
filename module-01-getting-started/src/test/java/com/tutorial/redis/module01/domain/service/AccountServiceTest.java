package com.tutorial.redis.module01.domain.service;

import com.tutorial.redis.module01.domain.model.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AccountService 領域服務單元測試
 * 驗證帳戶餘額相關的領域業務規則，包含餘額有效性檢查與充足性判斷。
 * 此服務為純領域邏輯，不依賴 Redis 或任何外部基礎設施。
 * 層級：Domain（領域服務層）
 */
@DisplayName("AccountService 領域服務單元測試")
class AccountServiceTest {

    private final AccountService accountService = new AccountService();

    // 驗證正數餘額判定為有效
    @Test
    @DisplayName("isBalanceValid_WhenPositiveBalance_ReturnsTrue")
    void isBalanceValid_WhenPositiveBalance_ReturnsTrue() {
        Account account = new Account("ACC-001", "Alice", new BigDecimal("100.00"), "USD", Instant.now());

        assertThat(accountService.isBalanceValid(account)).isTrue();
    }

    // 驗證零餘額判定為有效
    @Test
    @DisplayName("isBalanceValid_WhenZeroBalance_ReturnsTrue")
    void isBalanceValid_WhenZeroBalance_ReturnsTrue() {
        Account account = new Account("ACC-002", "Bob", BigDecimal.ZERO, "USD", Instant.now());

        assertThat(accountService.isBalanceValid(account)).isTrue();
    }

    // 驗證負數餘額判定為無效
    @Test
    @DisplayName("isBalanceValid_WhenNegativeBalance_ReturnsFalse")
    void isBalanceValid_WhenNegativeBalance_ReturnsFalse() {
        Account account = new Account("ACC-003", "Charlie", new BigDecimal("-50.00"), "USD", Instant.now());

        assertThat(accountService.isBalanceValid(account)).isFalse();
    }

    // 驗證餘額充足時回傳 true
    @Test
    @DisplayName("hasSufficientBalance_WhenEnough_ReturnsTrue")
    void hasSufficientBalance_WhenEnough_ReturnsTrue() {
        Account account = new Account("ACC-004", "Diana", new BigDecimal("500.00"), "USD", Instant.now());

        assertThat(accountService.hasSufficientBalance(account, new BigDecimal("300.00"))).isTrue();
    }

    // 驗證餘額剛好等於所需金額時回傳 true
    @Test
    @DisplayName("hasSufficientBalance_WhenExact_ReturnsTrue")
    void hasSufficientBalance_WhenExact_ReturnsTrue() {
        Account account = new Account("ACC-005", "Eve", new BigDecimal("100.00"), "USD", Instant.now());

        assertThat(accountService.hasSufficientBalance(account, new BigDecimal("100.00"))).isTrue();
    }

    // 驗證餘額不足時回傳 false
    @Test
    @DisplayName("hasSufficientBalance_WhenInsufficient_ReturnsFalse")
    void hasSufficientBalance_WhenInsufficient_ReturnsFalse() {
        Account account = new Account("ACC-006", "Frank", new BigDecimal("50.00"), "USD", Instant.now());

        assertThat(accountService.hasSufficientBalance(account, new BigDecimal("100.00"))).isFalse();
    }
}
