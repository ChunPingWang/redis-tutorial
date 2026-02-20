package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module03.domain.model.TimeSeriesDataPoint;
import com.tutorial.redis.module03.domain.port.outbound.TimeSeriesPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisTimeSeriesAdapter 整合測試")
class RedisTimeSeriesAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private TimeSeriesPort timeSeriesPort;

    private static final String TS_KEY = "temperature";
    private static final long BASE_TIMESTAMP = 1000000L;

    @Test
    @DisplayName("create_AndAdd_CanQueryRange — 建立時序並新增 5 個資料點後可查詢")
    void create_AndAdd_CanQueryRange() {
        timeSeriesPort.create(TS_KEY, 0, Map.of("sensor", "temp-01"));

        for (int i = 0; i < 5; i++) {
            timeSeriesPort.add(TS_KEY, BASE_TIMESTAMP + i * 1000, 20.0 + i);
        }

        List<TimeSeriesDataPoint> results = timeSeriesPort.range(
                TS_KEY, BASE_TIMESTAMP, BASE_TIMESTAMP + 4000);

        assertThat(results).hasSize(5);
        assertThat(results.get(0).getTimestamp()).isEqualTo(BASE_TIMESTAMP);
        assertThat(results.get(0).getValue()).isEqualTo(20.0);
        assertThat(results.get(4).getTimestamp()).isEqualTo(BASE_TIMESTAMP + 4000);
        assertThat(results.get(4).getValue()).isEqualTo(24.0);
    }

    @Test
    @DisplayName("add_WithAutoTimestamp_Succeeds — 使用自動時間戳新增不拋出例外")
    void add_WithAutoTimestamp_Succeeds() {
        timeSeriesPort.create(TS_KEY, 0, Map.of("sensor", "temp-01"));

        // Should not throw any exception
        timeSeriesPort.addAutoTimestamp(TS_KEY, 25.5);

        Optional<TimeSeriesDataPoint> latest = timeSeriesPort.getLatest(TS_KEY);
        assertThat(latest).isPresent();
        assertThat(latest.get().getValue()).isEqualTo(25.5);
    }

    @Test
    @DisplayName("range_WhenNoData_ReturnsEmptyList — 無資料時查詢回傳空列表")
    void range_WhenNoData_ReturnsEmptyList() {
        timeSeriesPort.create(TS_KEY, 0, Map.of("sensor", "temp-01"));

        List<TimeSeriesDataPoint> results = timeSeriesPort.range(TS_KEY, 0, Long.MAX_VALUE);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("getLatest_WhenDataExists_ReturnsLastPoint — 取得最新資料點")
    void getLatest_WhenDataExists_ReturnsLastPoint() {
        timeSeriesPort.create(TS_KEY, 0, Map.of("sensor", "temp-01"));
        timeSeriesPort.add(TS_KEY, BASE_TIMESTAMP, 20.0);
        timeSeriesPort.add(TS_KEY, BASE_TIMESTAMP + 1000, 21.5);
        timeSeriesPort.add(TS_KEY, BASE_TIMESTAMP + 2000, 23.0);

        Optional<TimeSeriesDataPoint> latest = timeSeriesPort.getLatest(TS_KEY);

        assertThat(latest).isPresent();
        assertThat(latest.get().getTimestamp()).isEqualTo(BASE_TIMESTAMP + 2000);
        assertThat(latest.get().getValue()).isEqualTo(23.0);
    }

    @Test
    @DisplayName("range_WithSubsetTimeWindow_ReturnsFilteredResults — 區間查詢回傳子集資料")
    void range_WithSubsetTimeWindow_ReturnsFilteredResults() {
        timeSeriesPort.create(TS_KEY, 0, Map.of("sensor", "temp-01"));

        // Add 10 data points
        for (int i = 0; i < 10; i++) {
            timeSeriesPort.add(TS_KEY, BASE_TIMESTAMP + i * 1000, 20.0 + i);
        }

        // Query middle 5 (indices 3-7, timestamps BASE+3000 to BASE+7000)
        List<TimeSeriesDataPoint> results = timeSeriesPort.range(
                TS_KEY, BASE_TIMESTAMP + 3000, BASE_TIMESTAMP + 7000);

        assertThat(results).hasSize(5);
        assertThat(results.get(0).getTimestamp()).isEqualTo(BASE_TIMESTAMP + 3000);
        assertThat(results.get(0).getValue()).isEqualTo(23.0);
        assertThat(results.get(4).getTimestamp()).isEqualTo(BASE_TIMESTAMP + 7000);
        assertThat(results.get(4).getValue()).isEqualTo(27.0);
    }
}
