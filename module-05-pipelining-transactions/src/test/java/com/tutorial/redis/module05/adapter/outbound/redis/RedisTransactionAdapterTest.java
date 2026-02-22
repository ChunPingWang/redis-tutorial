package com.tutorial.redis.module05.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module05.domain.model.TransferResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 交易 Adapter 整合測試 — 驗證透過 MULTI/EXEC 實現的帳戶轉帳交易功能。
 * 展示 Redis MULTI/EXEC 交易技術：將多個命令包裝為原子操作，確保轉帳的扣款與入帳一致性。
 * 所屬層級：Adapter 層（outbound），負責以 MULTI/EXEC 交易方式與 Redis 進行帳務操作。
 */
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

    // 驗證餘額充足時，MULTI/EXEC 交易成功完成轉帳並正確更新雙方餘額
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

    // 驗證餘額不足時，轉帳失敗且雙方餘額維持不變
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

    // 驗證來源帳戶不存在時，轉帳操作失敗
    @Test
    @DisplayName("transfer_WhenAccountNotExists_Fails — 帳戶不存在時轉帳失敗")
    void transfer_WhenAccountNotExists_Fails() {
        // Act — transfer from non-existent account "X" which has balance 0.0
        TransferResult result = adapter.transfer("X", "B", 100);

        // Assert
        assertThat(result.isSuccess()).isFalse();
    }
}
