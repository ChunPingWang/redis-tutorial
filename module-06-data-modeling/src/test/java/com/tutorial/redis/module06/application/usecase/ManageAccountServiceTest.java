package com.tutorial.redis.module06.application.usecase;

import com.tutorial.redis.module06.domain.model.AccountAggregate;
import com.tutorial.redis.module06.domain.port.outbound.AccountDaoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 測試 ManageAccountService 的應用層業務邏輯。
 * 使用 Mockito 模擬 AccountDaoPort，驗證 Service 正確委派給 Port 介面。
 * 屬於 Application 層（用例層），確認業務操作與 Redis 資料存取的解耦。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManageAccountService 單元測試")
class ManageAccountServiceTest {

    @Mock
    private AccountDaoPort accountDaoPort;

    @InjectMocks
    private ManageAccountService service;

    // 驗證建立帳戶時，Service 委派呼叫 AccountDaoPort.save 一次
    @Test
    @DisplayName("createAccount_DelegatesToPort — 建立帳戶應委派給 AccountDaoPort.save")
    void createAccount_DelegatesToPort() {
        // Arrange
        AccountAggregate account = new AccountAggregate(
                "acct-001", "Alice", 1000.00, "USD", Instant.now(), "ACTIVE"
        );

        // Act
        service.createAccount(account);

        // Assert
        verify(accountDaoPort, times(1)).save(account);
    }

    // 驗證查詢帳戶時，Service 委派呼叫 AccountDaoPort.findById 並回傳結果
    @Test
    @DisplayName("getAccount_DelegatesToPort — 查詢帳戶應委派給 AccountDaoPort.findById")
    void getAccount_DelegatesToPort() {
        // Arrange
        AccountAggregate account = new AccountAggregate(
                "acct-002", "Bob", 2000.00, "TWD", Instant.now(), "ACTIVE"
        );
        when(accountDaoPort.findById("acct-002")).thenReturn(Optional.of(account));

        // Act
        Optional<AccountAggregate> result = service.getAccount("acct-002");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getAccountId()).isEqualTo("acct-002");
        assertThat(result.get().getHolderName()).isEqualTo("Bob");
        verify(accountDaoPort, times(1)).findById("acct-002");
    }

    // 驗證依幣別查詢帳戶時，Service 委派呼叫 AccountDaoPort.findByCurrency
    @Test
    @DisplayName("findByCurrency_DelegatesToPort — 依幣別查詢應委派給 AccountDaoPort.findByCurrency")
    void findByCurrency_DelegatesToPort() {
        // Arrange
        List<AccountAggregate> expected = List.of(
                new AccountAggregate("acct-u1", "Alice", 1000.00, "USD", Instant.now(), "ACTIVE"),
                new AccountAggregate("acct-u2", "Bob", 2000.00, "USD", Instant.now(), "ACTIVE")
        );
        when(accountDaoPort.findByCurrency("USD")).thenReturn(expected);

        // Act
        List<AccountAggregate> result = service.findAccountsByCurrency("USD");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AccountAggregate::getCurrency)
                .containsOnly("USD");
        verify(accountDaoPort, times(1)).findByCurrency("USD");
    }
}
