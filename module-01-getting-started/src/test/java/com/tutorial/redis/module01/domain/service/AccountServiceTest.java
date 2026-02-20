package com.tutorial.redis.module01.domain.service;

import com.tutorial.redis.module01.domain.model.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccountService 領域服務單元測試")
class AccountServiceTest {

    private final AccountService accountService = new AccountService();

    @Test
    @DisplayName("isBalanceValid_WhenPositiveBalance_ReturnsTrue")
    void isBalanceValid_WhenPositiveBalance_ReturnsTrue() {
        Account account = new Account("ACC-001", "Alice", new BigDecimal("100.00"), "USD", Instant.now());

        assertThat(accountService.isBalanceValid(account)).isTrue();
    }

    @Test
    @DisplayName("isBalanceValid_WhenZeroBalance_ReturnsTrue")
    void isBalanceValid_WhenZeroBalance_ReturnsTrue() {
        Account account = new Account("ACC-002", "Bob", BigDecimal.ZERO, "USD", Instant.now());

        assertThat(accountService.isBalanceValid(account)).isTrue();
    }

    @Test
    @DisplayName("isBalanceValid_WhenNegativeBalance_ReturnsFalse")
    void isBalanceValid_WhenNegativeBalance_ReturnsFalse() {
        Account account = new Account("ACC-003", "Charlie", new BigDecimal("-50.00"), "USD", Instant.now());

        assertThat(accountService.isBalanceValid(account)).isFalse();
    }

    @Test
    @DisplayName("hasSufficientBalance_WhenEnough_ReturnsTrue")
    void hasSufficientBalance_WhenEnough_ReturnsTrue() {
        Account account = new Account("ACC-004", "Diana", new BigDecimal("500.00"), "USD", Instant.now());

        assertThat(accountService.hasSufficientBalance(account, new BigDecimal("300.00"))).isTrue();
    }

    @Test
    @DisplayName("hasSufficientBalance_WhenExact_ReturnsTrue")
    void hasSufficientBalance_WhenExact_ReturnsTrue() {
        Account account = new Account("ACC-005", "Eve", new BigDecimal("100.00"), "USD", Instant.now());

        assertThat(accountService.hasSufficientBalance(account, new BigDecimal("100.00"))).isTrue();
    }

    @Test
    @DisplayName("hasSufficientBalance_WhenInsufficient_ReturnsFalse")
    void hasSufficientBalance_WhenInsufficient_ReturnsFalse() {
        Account account = new Account("ACC-006", "Frank", new BigDecimal("50.00"), "USD", Instant.now());

        assertThat(accountService.hasSufficientBalance(account, new BigDecimal("100.00"))).isFalse();
    }
}
