package com.tutorial.redis.module02.application.usecase;

import com.tutorial.redis.module02.domain.model.Transaction;
import com.tutorial.redis.module02.domain.model.Transaction.TransactionType;
import com.tutorial.redis.module02.domain.port.outbound.TransactionLogPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 交易記錄管理 Use Case 單元測試
 * 驗證 ManageTransactionLogService 正確委派操作至 TransactionLogPort（Redis List）。
 * 包含 LPUSH 新增交易與 LTRIM 裁剪日誌的組合邏輯。
 * 層級：Application（Use Case 業務邏輯）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManageTransactionLogService 單元測試")
class ManageTransactionLogServiceTest {

    @Mock
    private TransactionLogPort transactionLogPort;

    @InjectMocks
    private ManageTransactionLogService service;

    private Transaction createTransaction(String txId) {
        return new Transaction(txId, "ACC-001", new BigDecimal("100.00"),
                TransactionType.DEPOSIT, Instant.now(), "Test");
    }

    // 驗證記錄交易時依序呼叫 addTransaction（LPUSH）與 trimToSize（LTRIM）
    @Test
    @DisplayName("recordTransaction_AddsAndTrims — 新增交易後裁剪至 MAX_LOG_SIZE")
    void recordTransaction_AddsAndTrims() {
        Transaction tx = createTransaction("TX-001");

        service.recordTransaction("ACC-001", tx);

        verify(transactionLogPort).addTransaction("ACC-001", tx);
        verify(transactionLogPort).trimToSize("ACC-001", ManageTransactionLogService.MAX_LOG_SIZE);
    }

    // 驗證取得最近交易時正確委派至 Port 的 getRecentTransactions
    @Test
    @DisplayName("getRecentTransactions_DelegatesToPort — 委派至 Port 的 getRecentTransactions 方法")
    void getRecentTransactions_DelegatesToPort() {
        Transaction tx = createTransaction("TX-001");
        when(transactionLogPort.getRecentTransactions("ACC-001", 10))
                .thenReturn(List.of(tx));

        List<Transaction> result = service.getRecentTransactions("ACC-001", 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionId()).isEqualTo("TX-001");
        verify(transactionLogPort).getRecentTransactions("ACC-001", 10);
    }

    // 驗證查詢交易筆數時正確委派至 Port 的 getTransactionCount
    @Test
    @DisplayName("getTransactionCount_DelegatesToPort — 委派至 Port 的 getTransactionCount 方法")
    void getTransactionCount_DelegatesToPort() {
        when(transactionLogPort.getTransactionCount("ACC-001")).thenReturn(42L);

        long count = service.getTransactionCount("ACC-001");

        assertThat(count).isEqualTo(42);
        verify(transactionLogPort).getTransactionCount("ACC-001");
    }
}
