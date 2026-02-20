package com.tutorial.redis.module05.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module05.domain.model.TransferResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisTransactionAdapter 整合測試")
class RedisTransactionAdapterTest extends AbstractRedisIntegrationTest {

    private static final String ACCOUNT_A_KEY = "account:balance:A";
    private static final String ACCOUNT_B_KEY = "account:balance:B";

    @Autowired
    private RedisTransactionAdapter adapter;

    @BeforeEach
    void setUpAccounts() {
        stringRedisTemplate.opsForValue().set(ACCOUNT_A_KEY, "1000");
        stringRedisTemplate.opsForValue().set(ACCOUNT_B_KEY, "500");
    }

    @Test
    @DisplayName("transfer_WhenSufficientBalance_Succeeds — 餘額充足時轉帳成功")
    void transfer_WhenSufficientBalance_Succeeds() {
        // Act
        TransferResult result = adapter.transfer("A", "B", 200);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAmount()).isEqualTo(200);
        assertThat(result.getFromAccountId()).isEqualTo("A");
        assertThat(result.getToAccountId()).isEqualTo("B");

        // Verify balances in Redis
        String fromBalance = stringRedisTemplate.opsForValue().get(ACCOUNT_A_KEY);
        String toBalance = stringRedisTemplate.opsForValue().get(ACCOUNT_B_KEY);
        assertThat(Double.parseDouble(fromBalance)).isEqualTo(800.0);
        assertThat(Double.parseDouble(toBalance)).isEqualTo(700.0);
    }

    @Test
    @DisplayName("transfer_WhenInsufficientBalance_Fails — 餘額不足時轉帳失敗且餘額不變")
    void transfer_WhenInsufficientBalance_Fails() {
        // Act
        TransferResult result = adapter.transfer("A", "B", 2000);

        // Assert
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Insufficient balance");

        // Verify balances unchanged
        String fromBalance = stringRedisTemplate.opsForValue().get(ACCOUNT_A_KEY);
        String toBalance = stringRedisTemplate.opsForValue().get(ACCOUNT_B_KEY);
        assertThat(Double.parseDouble(fromBalance)).isEqualTo(1000.0);
        assertThat(Double.parseDouble(toBalance)).isEqualTo(500.0);
    }

    @Test
    @DisplayName("transfer_WhenAccountNotExists_Fails — 帳戶不存在時轉帳失敗")
    void transfer_WhenAccountNotExists_Fails() {
        // Act — transfer from non-existent account "X" which has balance 0.0
        TransferResult result = adapter.transfer("X", "B", 100);

        // Assert
        assertThat(result.isSuccess()).isFalse();
    }
}
