package com.tutorial.redis.module02.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module02.domain.model.Transaction;
import com.tutorial.redis.module02.domain.model.Transaction.TransactionType;
import com.tutorial.redis.module02.domain.port.outbound.TransactionLogPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 交易記錄 Adapter 整合測試
 * 驗證使用 Redis List（LPUSH/LRANGE/LTRIM/LLEN）實作交易日誌的 Capped Collection。
 * 層級：Adapter（外部端口實作）
 */
@DisplayName("RedisTransactionLogAdapter 整合測試")
class RedisTransactionLogAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private TransactionLogPort transactionLogPort;

    private Transaction createTransaction(String txId, String accountId, TransactionType type) {
        return new Transaction(txId, accountId, new BigDecimal("100.00"),
                type, Instant.now(), "Test transaction " + txId);
    }

    // 驗證 LPUSH 將新交易推入列表頭部，最新交易排在最前面
    @Test
    @DisplayName("addTransaction_WhenCalled_PrependToList — LPUSH 行為，最新交易在最前")
    void addTransaction_WhenCalled_PrependToList() {
        String accountId = "ACC-001";
        Transaction tx1 = createTransaction("TX-001", accountId, TransactionType.DEPOSIT);
        Transaction tx2 = createTransaction("TX-002", accountId, TransactionType.WITHDRAWAL);
        Transaction tx3 = createTransaction("TX-003", accountId, TransactionType.TRANSFER);

        transactionLogPort.addTransaction(accountId, tx1);
        transactionLogPort.addTransaction(accountId, tx2);
        transactionLogPort.addTransaction(accountId, tx3);

        List<Transaction> recent = transactionLogPort.getRecentTransactions(accountId, 3);
        assertThat(recent).hasSize(3);
        assertThat(recent.get(0).getTransactionId()).isEqualTo("TX-003");
        assertThat(recent.get(1).getTransactionId()).isEqualTo("TX-002");
        assertThat(recent.get(2).getTransactionId()).isEqualTo("TX-001");
    }

    // 驗證 LRANGE 取得最近 N 筆交易，按時間倒序排列
    @Test
    @DisplayName("getRecentTransactions_WhenMultiple_ReturnsLatestFirst — 取得最近交易記錄")
    void getRecentTransactions_WhenMultiple_ReturnsLatestFirst() {
        String accountId = "ACC-002";
        for (int i = 1; i <= 5; i++) {
            transactionLogPort.addTransaction(accountId,
                    createTransaction("TX-" + i, accountId, TransactionType.DEPOSIT));
        }

        List<Transaction> recent = transactionLogPort.getRecentTransactions(accountId, 3);

        assertThat(recent).hasSize(3);
        assertThat(recent.get(0).getTransactionId()).isEqualTo("TX-5");
        assertThat(recent.get(1).getTransactionId()).isEqualTo("TX-4");
        assertThat(recent.get(2).getTransactionId()).isEqualTo("TX-3");
    }

    // 驗證 LTRIM 裁剪列表至指定大小，僅保留最近的交易記錄
    @Test
    @DisplayName("trimToSize_WhenExceedsMax_KeepsOnlyRecent — 裁剪至指定大小")
    void trimToSize_WhenExceedsMax_KeepsOnlyRecent() {
        String accountId = "ACC-003";
        for (int i = 1; i <= 5; i++) {
            transactionLogPort.addTransaction(accountId,
                    createTransaction("TX-" + i, accountId, TransactionType.DEPOSIT));
        }

        transactionLogPort.trimToSize(accountId, 3);

        assertThat(transactionLogPort.getTransactionCount(accountId)).isEqualTo(3);
        List<Transaction> remaining = transactionLogPort.getRecentTransactions(accountId, 5);
        assertThat(remaining).hasSize(3);
        assertThat(remaining.get(0).getTransactionId()).isEqualTo("TX-5");
        assertThat(remaining.get(2).getTransactionId()).isEqualTo("TX-3");
    }

    // 驗證 LLEN 回傳列表中的交易筆數
    @Test
    @DisplayName("getTransactionCount_ReturnsCorrectCount — 回傳正確的交易筆數")
    void getTransactionCount_ReturnsCorrectCount() {
        String accountId = "ACC-004";
        transactionLogPort.addTransaction(accountId,
                createTransaction("TX-001", accountId, TransactionType.DEPOSIT));
        transactionLogPort.addTransaction(accountId,
                createTransaction("TX-002", accountId, TransactionType.WITHDRAWAL));

        long count = transactionLogPort.getTransactionCount(accountId);

        assertThat(count).isEqualTo(2);
    }

    // 驗證查詢空帳戶時 LRANGE 回傳空列表
    @Test
    @DisplayName("getRecentTransactions_WhenEmpty_ReturnsEmptyList — 空帳戶回傳空列表")
    void getRecentTransactions_WhenEmpty_ReturnsEmptyList() {
        List<Transaction> recent = transactionLogPort.getRecentTransactions("ACC-EMPTY", 10);

        assertThat(recent).isEmpty();
    }

    // 驗證 Redis key 遵循 banking:txlog:{accountId} 命名規範
    @Test
    @DisplayName("key_FollowsNamingConvention — 驗證 key 符合 banking:txlog:* 命名規範")
    void key_FollowsNamingConvention() {
        String accountId = "ACC-KEY-001";
        transactionLogPort.addTransaction(accountId,
                createTransaction("TX-001", accountId, TransactionType.DEPOSIT));

        Set<String> keys = stringRedisTemplate.keys("banking:txlog:" + accountId);
        assertThat(keys).isNotNull().hasSize(1);
        assertThat(keys.iterator().next()).isEqualTo("banking:txlog:ACC-KEY-001");
    }
}
