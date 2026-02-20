package com.tutorial.redis.module06.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module06.domain.model.ExchangeRateSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisExchangeRateTimeSeriesAdapter 整合測試")
class RedisExchangeRateTimeSeriesAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisExchangeRateTimeSeriesAdapter adapter;

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

    @Test
    @DisplayName("getSnapshots_WhenEmpty_ReturnsEmptyList — 無資料時查詢應回傳空列表")
    void getSnapshots_WhenEmpty_ReturnsEmptyList() {
        // Act
        List<ExchangeRateSnapshot> result = adapter.getSnapshots("GBP/JPY", 0, Long.MAX_VALUE);

        // Assert
        assertThat(result).isEmpty();
    }
}
