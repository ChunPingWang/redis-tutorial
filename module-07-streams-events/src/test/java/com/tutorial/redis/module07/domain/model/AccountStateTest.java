package com.tutorial.redis.module07.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 驗證 AccountState 領域模型的狀態轉換邏輯。
 * 測試透過 apply 方法套用各類帳戶事件（存款、提款、凍結）後的狀態變化，
 * 此為 Event Sourcing 模式中狀態重建的核心邏輯。
 * 所屬層級：Domain 層（領域模型單元測試）
 */
@DisplayName("AccountState 領域模型測試")
class AccountStateTest {

    // 驗證帳戶初始狀態的預設值：餘額為零、狀態為 ACTIVE、事件計數為零
    @Test
    @DisplayName("initial_CreatesZeroBalanceActiveState — 初始狀態應為 balance=0、status=ACTIVE、eventCount=0")
    void initial_CreatesZeroBalanceActiveState() {
        // Act
        AccountState state = AccountState.initial("acc-001");

        // Assert
        assertThat(state.getAccountId()).isEqualTo("acc-001");
        assertThat(state.getBalance()).isEqualTo(0.0);
        assertThat(state.getStatus()).isEqualTo("ACTIVE");
        assertThat(state.getEventCount()).isEqualTo(0);
    }

    // 驗證套用 MONEY_DEPOSITED 事件後，餘額正確增加
    @Test
    @DisplayName("apply_MoneyDeposited_IncreasesBalance — 初始狀態存入 100 後，餘額應為 100")
    void apply_MoneyDeposited_IncreasesBalance() {
        // Arrange
        AccountState initial = AccountState.initial("acc-001");
        AccountEvent deposit = new AccountEvent(
                "evt-1", "acc-001", "MONEY_DEPOSITED", 100.0, Instant.now(), Map.of());

        // Act
        AccountState result = initial.apply(deposit);

        // Assert
        assertThat(result.getBalance()).isEqualTo(100.0);
        assertThat(result.getEventCount()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    // 驗證套用 MONEY_WITHDRAWN 事件後，餘額正確減少
    @Test
    @DisplayName("apply_MoneyWithdrawn_DecreasesBalance — 存入 100 後提取 30，餘額應為 70")
    void apply_MoneyWithdrawn_DecreasesBalance() {
        // Arrange
        AccountState initial = AccountState.initial("acc-001");
        AccountEvent deposit = new AccountEvent(
                "evt-1", "acc-001", "MONEY_DEPOSITED", 100.0, Instant.now(), Map.of());
        AccountEvent withdraw = new AccountEvent(
                "evt-2", "acc-001", "MONEY_WITHDRAWN", 30.0, Instant.now(), Map.of());

        // Act
        AccountState afterDeposit = initial.apply(deposit);
        AccountState afterWithdraw = afterDeposit.apply(withdraw);

        // Assert
        assertThat(afterWithdraw.getBalance()).isEqualTo(70.0);
        assertThat(afterWithdraw.getEventCount()).isEqualTo(2);
        assertThat(afterWithdraw.getStatus()).isEqualTo("ACTIVE");
    }

    // 驗證套用 ACCOUNT_FROZEN 事件後，帳戶狀態從 ACTIVE 變為 FROZEN
    @Test
    @DisplayName("apply_AccountFrozen_ChangesStatus — 套用凍結事件後，狀態應變為 FROZEN")
    void apply_AccountFrozen_ChangesStatus() {
        // Arrange
        AccountState initial = AccountState.initial("acc-001");
        AccountEvent frozen = new AccountEvent(
                "evt-1", "acc-001", "ACCOUNT_FROZEN", null, Instant.now(), Map.of());

        // Act
        AccountState result = initial.apply(frozen);

        // Assert
        assertThat(result.getStatus()).isEqualTo("FROZEN");
        assertThat(result.getBalance()).isEqualTo(0.0);
        assertThat(result.getEventCount()).isEqualTo(1);
    }
}
