package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisTransactionRankingAdapter 整合測試類別。
 * 驗證使用 Redis Sorted Set 實作交易金額排行榜的功能。
 * 展示 ZADD/ZREVRANGE 在金融交易排名場景的應用。
 * 所屬：金融子系統 — adapter 層
 */
@DisplayName("RedisTransactionRankingAdapter 整合測試")
class RedisTransactionRankingAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisTransactionRankingAdapter adapter;

    // 驗證加入多筆不同金額的交易後，取得 Top N 應按金額由高到低排序
    @Test
    @DisplayName("addAndGetTop_ReturnsHighestAmounts — 加入多筆交易後取得 Top N 應回傳金額最高者")
    void addAndGetTop_ReturnsHighestAmounts() {
        // Arrange — add transactions with varying amounts
        adapter.addToLeaderboard("tx-001", 1000.0);
        adapter.addToLeaderboard("tx-002", 5000.0);
        adapter.addToLeaderboard("tx-003", 3000.0);
        adapter.addToLeaderboard("tx-004", 8000.0);
        adapter.addToLeaderboard("tx-005", 2000.0);

        // Act — get top 3
        List<String> top3 = adapter.getTopN(3);

        // Assert — should be ordered by amount descending
        assertThat(top3).hasSize(3);
        assertThat(top3.get(0)).isEqualTo("tx-004"); // 8000
        assertThat(top3.get(1)).isEqualTo("tx-002"); // 5000
        assertThat(top3.get(2)).isEqualTo("tx-003"); // 3000
    }
}
