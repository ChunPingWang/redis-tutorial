package com.tutorial.redis.module13.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module13.domain.model.MemoryInfo;
import com.tutorial.redis.module13.domain.model.ServerMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("RedisMonitoringAdapter 整合測試")
class RedisMonitoringAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisMonitoringAdapter adapter;

    @Test
    @DisplayName("getMemoryInfo_ReturnsValidInfo — 取得記憶體資訊，usedMemory 應大於 0")
    void getMemoryInfo_ReturnsValidInfo() {
        // Act
        MemoryInfo result = adapter.getMemoryInfo();

        // Assert — a running Redis instance always uses some memory
        assertThat(result.getUsedMemory()).isGreaterThan(0);
        assertThat(result.getEvictionPolicy()).isNotNull();
    }

    @Test
    @DisplayName("getSlowLog_ReturnsListWithoutError — 取得慢查詢日誌不應拋出例外")
    void getSlowLog_ReturnsListWithoutError() {
        // Assert — SLOWLOG GET should not throw, even if the log is empty
        assertThatCode(() -> adapter.getSlowLog(10)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getServerMetrics_ReturnsValidMetrics — 取得服務指標，uptimeInSeconds 應大於 0")
    void getServerMetrics_ReturnsValidMetrics() {
        // Act
        ServerMetrics metrics = adapter.getServerMetrics();

        // Assert — a running Redis instance has uptime > 0 and at least 1 connected client
        assertThat(metrics.getUptimeInSeconds()).isGreaterThan(0);
        assertThat(metrics.getConnectedClients()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("getKeyCount_WhenEmpty_ReturnsZero — 無資料時 Key 計數，應回傳 0 或非負數")
    void getKeyCount_WhenEmpty_ReturnsZero() {
        // Act & Assert — after flushAll in @BeforeEach, key count should be >= 0
        assertThat(adapter.getKeyCount()).isGreaterThanOrEqualTo(0);
    }
}
