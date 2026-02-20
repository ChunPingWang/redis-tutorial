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

@ExtendWith(MockitoExtension.class)
@DisplayName("ManageAccountService 單元測試")
class ManageAccountServiceTest {

    @Mock
    private AccountDaoPort accountDaoPort;

    @InjectMocks
    private ManageAccountService service;

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
