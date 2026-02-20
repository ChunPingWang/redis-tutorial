package com.tutorial.redis.module14.finance.application.usecase;

import com.tutorial.redis.module14.finance.domain.port.outbound.AccountCachePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AccountService 單元測試")
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountCachePort accountCachePort;

    @InjectMocks
    private AccountService service;

    @Test
    @DisplayName("cacheAccountBalance_DelegatesToPort — 快取餘額應委派給 AccountCachePort")
    void cacheAccountBalance_DelegatesToPort() {
        // Act
        service.cacheAccountBalance("acc-001", 1500.50);

        // Assert — verify the port was called with correct arguments
        verify(accountCachePort).setBalance("acc-001", 1500.50);
    }

    @Test
    @DisplayName("getAccountBalance_DelegatesToPort — 取得餘額應委派給 AccountCachePort")
    void getAccountBalance_DelegatesToPort() {
        // Arrange
        when(accountCachePort.getBalance("acc-001")).thenReturn(2500.75);

        // Act
        Double result = service.getAccountBalance("acc-001");

        // Assert
        verify(accountCachePort).getBalance("acc-001");
        assertThat(result).isEqualTo(2500.75);
    }
}
