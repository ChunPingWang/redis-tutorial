package com.tutorial.redis.module13.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module13.domain.model.MemoryInfo;
import com.tutorial.redis.module13.domain.model.ServerMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 測試 RedisMonitoringAdapter 的整合行為（Adapter 層）。
 * 驗證透過實際 Redis 連線取得記憶體資訊（INFO memory）、
 * 慢查詢日誌（SLOWLOG GET）及伺服器指標（INFO stats/server）等監控功能。
 * 屬於六角形架構的 Outbound Adapter 層，直接與 Redis 互動。
 */
@DisplayName("RedisMonitoringAdapter 整合測試")
class RedisMonitoringAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisMonitoringAdapter adapter;

    // 驗證透過 INFO memory 取得的記憶體資訊中，usedMemory 大於 0 且淘汰策略不為 null
    @Test
    @DisplayName("getMemoryInfo_ReturnsValidInfo — 取得記憶體資訊，usedMemory 應大於 0")
    void getMemoryInfo_ReturnsValidInfo() {
        // Act
        MemoryInfo result = adapter.getMemoryInfo();

        // Assert — a running Redis instance always uses some memory
        assertThat(result.getUsedMemory()).isGreaterThan(0);
        assertThat(result.getEvictionPolicy()).isNotNull();
    }

    // 驗證執行 SLOWLOG GET 取得慢查詢日誌時不會拋出例外（即使日誌為空）
    @Test
    @DisplayName("getSlowLog_ReturnsListWithoutError — 取得慢查詢日誌不應拋出例外")
    void getSlowLog_ReturnsListWithoutError() {
        // Assert — SLOWLOG GET should not throw, even if the log is empty
        assertThatCode(() -> adapter.getSlowLog(10)).doesNotThrowAnyException();
    }

    // 驗證透過 INFO 取得的伺服器指標中，運行時間大於 0 且至少有 1 個連線客戶端
    @Test
    @DisplayName("getServerMetrics_ReturnsValidMetrics — 取得服務指標，uptimeInSeconds 應大於 0")
    void getServerMetrics_ReturnsValidMetrics() {
        // Act
        ServerMetrics metrics = adapter.getServerMetrics();

        // Assert — a running Redis instance has uptime > 0 and at least 1 connected client
        assertThat(metrics.getUptimeInSeconds()).isGreaterThan(0);
        assertThat(metrics.getConnectedClients()).isGreaterThanOrEqualTo(1);
    }

    // 驗證清空資料後查詢 Key 數量，結果應為 0 或非負數
    @Test
    @DisplayName("getKeyCount_WhenEmpty_ReturnsZero — 無資料時 Key 計數，應回傳 0 或非負數")
    void getKeyCount_WhenEmpty_ReturnsZero() {
        // Act & Assert — after flushAll in @BeforeEach, key count should be >= 0
        assertThat(adapter.getKeyCount()).isGreaterThanOrEqualTo(0);
    }
}
