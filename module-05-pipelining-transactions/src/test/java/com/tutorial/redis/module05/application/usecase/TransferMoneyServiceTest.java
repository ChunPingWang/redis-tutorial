package com.tutorial.redis.module05.application.usecase;

import com.tutorial.redis.module05.domain.model.TransferResult;
import com.tutorial.redis.module05.domain.port.outbound.TransactionPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 轉帳 Service 單元測試 — 驗證 Application 層正確委派轉帳操作給 TransactionPort。
 * 展示 MULTI/EXEC 交易的應用層邏輯：Service 不含業務實作，僅轉發轉帳請求至 Port。
 * 所屬層級：Application 層（use case），使用 Mockito 模擬 Port 進行隔離測試。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransferMoneyService 單元測試")
class TransferMoneyServiceTest {

    @Mock
    private TransactionPort transactionPort;

    @InjectMocks
    private TransferMoneyService service;

    // 驗證轉帳操作正確委派給 TransactionPort 並回傳預期結果
    @Test
    @DisplayName("transfer_DelegatesToPort — 轉帳委派給 TransactionPort 執行")
    void transfer_DelegatesToPort() {
        // Arrange
        TransferResult expected = new TransferResult("A", "B", 200, true, "Transfer successful");
        when(transactionPort.transfer("A", "B", 200)).thenReturn(expected);

        // Act
        TransferResult result = service.transfer("A", "B", 200);

        // Assert
        assertThat(result).isEqualTo(expected);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFromAccountId()).isEqualTo("A");
        assertThat(result.getToAccountId()).isEqualTo("B");
        assertThat(result.getAmount()).isEqualTo(200);
        verify(transactionPort, times(1)).transfer("A", "B", 200);
    }
}
