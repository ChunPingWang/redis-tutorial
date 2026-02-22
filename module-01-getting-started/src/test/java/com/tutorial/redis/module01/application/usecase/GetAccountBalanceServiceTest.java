package com.tutorial.redis.module01.application.usecase;

import com.tutorial.redis.module01.domain.model.Account;
import com.tutorial.redis.module01.domain.port.outbound.AccountCachePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 帳戶餘額查詢 Use Case 單元測試
 * 驗證 GetAccountBalanceService 透過 AccountCachePort 查詢快取帳戶，
 * 確認快取命中與未命中時的行為正確。
 * 層級：Application（Use Case 層）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetAccountBalanceService 單元測試")
class GetAccountBalanceServiceTest {

    @Mock
    private AccountCachePort accountCachePort;

    @InjectMocks
    private GetAccountBalanceService service;

    // 驗證帳戶已快取時回傳正確帳戶資料（快取命中）
    @Test
    @DisplayName("getAccount_WhenAccountCached_ReturnsAccount")
    void getAccount_WhenAccountCached_ReturnsAccount() {
        Account account = new Account("ACC-001", "Alice", new BigDecimal("5000.00"), "USD", Instant.now());
        when(accountCachePort.findById("ACC-001")).thenReturn(Optional.of(account));

        Optional<Account> result = service.getAccount("ACC-001");

        assertThat(result).isPresent();
        assertThat(result.get().getHolderName()).isEqualTo("Alice");
        verify(accountCachePort).findById("ACC-001");
    }

    // 驗證帳戶未快取時回傳空值（快取未命中）
    @Test
    @DisplayName("getAccount_WhenAccountNotCached_ReturnsEmpty")
    void getAccount_WhenAccountNotCached_ReturnsEmpty() {
        when(accountCachePort.findById("ACC-999")).thenReturn(Optional.empty());

        Optional<Account> result = service.getAccount("ACC-999");

        assertThat(result).isEmpty();
        verify(accountCachePort).findById("ACC-999");
    }
}
