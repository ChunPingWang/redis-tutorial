package com.tutorial.redis.module02.domain.model;

import com.tutorial.redis.module02.domain.model.Transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Transaction 領域模型測試")
class TransactionTest {

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

    @Test
    @DisplayName("constructor_WhenNullTransactionId_ThrowsNPE — null transactionId 拋出 NullPointerException")
    void constructor_WhenNullTransactionId_ThrowsNPE() {
        assertThatThrownBy(() -> new Transaction(null, "ACC-001", new BigDecimal("100.00"),
                TransactionType.DEPOSIT, Instant.now(), "Test"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transactionId");
    }

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
