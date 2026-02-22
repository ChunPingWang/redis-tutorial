package com.tutorial.redis.module14.finance.application.usecase;

import com.tutorial.redis.module14.finance.domain.model.Transaction;
import com.tutorial.redis.module14.finance.domain.port.outbound.TransactionRankingPort;
import com.tutorial.redis.module14.finance.domain.port.outbound.TransactionSearchPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TransactionService 應用層單元測試類別。
 * 驗證交易記錄（排行榜+索引）與排名查詢的業務邏輯。
 * 展示 Redis Sorted Set 排行榜與 RediSearch 索引的協同應用。
 * 所屬：金融子系統 — application 層
 */
@DisplayName("TransactionService 單元測試")
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRankingPort rankingPort;

    @Mock
    private TransactionSearchPort searchPort;

    @InjectMocks
    private TransactionService service;

    // 驗證記錄交易時同時加入 Sorted Set 排行榜與搜尋索引
    @Test
    @DisplayName("recordTransaction_AddsToLeaderboardAndIndex — 記錄交易應同時加入排行榜和索引")
    void recordTransaction_AddsToLeaderboardAndIndex() {
        // Arrange
        Transaction tx = new Transaction("tx-001", "acc-001", "acc-002",
                5000.0, "USD", System.currentTimeMillis(), "COMPLETED");

        // Act
        service.recordTransaction(tx);

        // Assert — verify both ports were called
        verify(rankingPort).addToLeaderboard("tx-001", 5000.0);
        verify(searchPort).indexTransaction(tx);
    }

    // 驗證取得交易排名時，正確委派給 TransactionRankingPort 並回傳排序結果
    @Test
    @DisplayName("getTopTransactions_DelegatesToRankingPort — 取得排名應委派給 TransactionRankingPort")
    void getTopTransactions_DelegatesToRankingPort() {
        // Arrange
        when(rankingPort.getTopN(3)).thenReturn(List.of("tx-003", "tx-001", "tx-002"));

        // Act
        List<String> result = service.getTopTransactions(3);

        // Assert
        verify(rankingPort).getTopN(3);
        assertThat(result).containsExactly("tx-003", "tx-001", "tx-002");
    }
}
