package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisExchangeRateAdapter 整合測試")
class RedisExchangeRateAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private RedisExchangeRateAdapter adapter;

    @Test
    @DisplayName("recordAndGetLatest_ReturnsLatestRate — 記錄匯率後取得最新值應正確回傳")
    void recordAndGetLatest_ReturnsLatestRate() {
        // Arrange — record multiple exchange rates at different timestamps
        String pair = "USD/EUR";
        adapter.recordRate(pair, 0.85, 1700000000L);
        adapter.recordRate(pair, 0.86, 1700001000L);
        adapter.recordRate(pair, 0.87, 1700002000L);

        // Act — get the latest rate
        Double latestRate = adapter.getLatestRate(pair);

        // Assert — should return the most recent rate
        assertThat(latestRate).isNotNull();
        assertThat(latestRate).isEqualTo(0.87);
    }
}
