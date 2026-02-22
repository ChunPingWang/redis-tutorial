package com.tutorial.redis.module02.domain.model;

import com.tutorial.redis.module02.domain.model.Transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 交易紀錄領域模型測試
 * 驗證 Transaction 的建構子驗證邏輯與 TransactionType 列舉值。
 * 對應 Redis List 中序列化儲存的交易物件。
 * 層級：Domain（領域模型）
 */
@DisplayName("Transaction 領域模型測試")
class TransactionTest {

    // 驗證使用合法參數建構 Transaction 後各欄位值正確
    @Test
    @DisplayName("constructor_WhenValidArgs_CreatesTransaction — 建立有效的交易紀錄")
    void constructor_WhenValidArgs_CreatesTransaction() {
        Instant now = Instant.now();
        Transaction tx = new Transaction("TX-001", "ACC-001", new BigDecimal("500.00"),
                TransactionType.DEPOSIT, now, "Salary deposit");

        assertThat(tx.getTransactionId()).isEqualTo("TX-001");
        assertThat(tx.getAccountId()).isEqualTo("ACC-001");
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(tx.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(tx.getTimestamp()).isEqualTo(now);
        assertThat(tx.getDescription()).isEqualTo("Salary deposit");
    }

    // 驗證 transactionId 為 null 時拋出 NullPointerException
    @Test
    @DisplayName("constructor_WhenNullTransactionId_ThrowsNPE — null transactionId 拋出 NullPointerException")
    void constructor_WhenNullTransactionId_ThrowsNPE() {
        assertThatThrownBy(() -> new Transaction(null, "ACC-001", new BigDecimal("100.00"),
                TransactionType.DEPOSIT, Instant.now(), "Test"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transactionId");
    }

    // 驗證 TransactionType 列舉包含 DEPOSIT、WITHDRAWAL、TRANSFER 三種類型
    @Test
    @DisplayName("type_Enum_HasExpectedValues — TransactionType 包含 DEPOSIT, WITHDRAWAL, TRANSFER")
    void type_Enum_HasExpectedValues() {
        TransactionType[] values = TransactionType.values();

        assertThat(values).hasSize(3);
        assertThat(values).containsExactly(
                TransactionType.DEPOSIT,
                TransactionType.WITHDRAWAL,
                TransactionType.TRANSFER
        );
    }
}
