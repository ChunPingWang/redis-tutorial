package com.tutorial.redis.module06.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module06.domain.model.ExchangeRateSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試 RedisExchangeRateTimeSeriesAdapter 的匯率時間序列整合行為。
 * 驗證使用 Sorted Set 以時間戳為 score 儲存匯率快照，並支援時間範圍查詢。
 * 屬於 Adapter 層（外部介面卡），示範 Sorted Set 時間索引的資料建模模式。
 */
@DisplayName("RedisExchangeRateTimeSeriesAdapter 整合測試")
class RedisExchangeRateTimeSeriesAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisExchangeRateTimeSeriesAdapter adapter;

    // 驗證新增多筆快照後，以 ZRANGEBYSCORE 做時間範圍查詢回傳正確結果
    @Test
    @DisplayName("addAndQuery_ReturnsSnapshotsInRange — 新增 5 筆快照，查詢 1500~3500 應回傳 2 筆")
    void addAndQuery_ReturnsSnapshotsInRange() {
        // Arrange
        String pair = "USD/TWD";
        adapter.addSnapshot(new ExchangeRateSnapshot(pair, 31.10, 1000));
        adapter.addSnapshot(new ExchangeRateSnapshot(pair, 31.20, 2000));
        adapter.addSnapshot(new ExchangeRateSnapshot(pair, 31.30, 3000));
        adapter.addSnapshot(new ExchangeRateSnapshot(pair, 31.40, 4000));
        adapter.addSnapshot(new ExchangeRateSnapshot(pair, 31.50, 5000));

        // Act
        List<ExchangeRateSnapshot> result = adapter.getSnapshots(pair, 1500, 3500);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTimestamp()).isEqualTo(2000);
        assertThat(result.get(0).getRate()).isEqualTo(31.20);
        assertThat(result.get(1).getTimestamp()).isEqualTo(3000);
        assertThat(result.get(1).getRate()).isEqualTo(31.30);

        // Verify all snapshots are retrievable
        List<ExchangeRateSnapshot> all = adapter.getSnapshots(pair, 0, 10000);
        assertThat(all).hasSize(5);
    }

    // 驗證取得最新快照時，回傳 Sorted Set 中 score 最大（最新時間戳）的成員
    @Test
    @DisplayName("getLatestSnapshot_ReturnsLatest — 新增 3 筆快照，應回傳時間戳最大的一筆")
    void getLatestSnapshot_ReturnsLatest() {
        // Arrange
        String pair = "EUR/USD";
        adapter.addSnapshot(new ExchangeRateSnapshot(pair, 1.0850, 1000));
        adapter.addSnapshot(new ExchangeRateSnapshot(pair, 1.0870, 2000));
        adapter.addSnapshot(new ExchangeRateSnapshot(pair, 1.0900, 3000));

        // Act
        Optional<ExchangeRateSnapshot> latest = adapter.getLatestSnapshot(pair);

        // Assert
        assertThat(latest).isPresent();
        assertThat(latest.get().getTimestamp()).isEqualTo(3000);
        assertThat(latest.get().getRate()).isEqualTo(1.0900);
        assertThat(latest.get().getCurrencyPair()).isEqualTo("EUR/USD");
    }

    // 驗證無任何快照資料時，查詢回傳空列表而非拋出例外
    @Test
    @DisplayName("getSnapshots_WhenEmpty_ReturnsEmptyList — 無資料時查詢應回傳空列表")
    void getSnapshots_WhenEmpty_ReturnsEmptyList() {
        // Act
        List<ExchangeRateSnapshot> result = adapter.getSnapshots("GBP/JPY", 0, Long.MAX_VALUE);

        // Assert
        assertThat(result).isEmpty();
    }
}
