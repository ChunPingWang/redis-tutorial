package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module14.finance.domain.model.RiskAlert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisRiskAlertStreamAdapter 整合測試類別。
 * 驗證使用 Redis Stream 發布與消費風險警報事件的功能。
 * 展示 XADD/XREADGROUP 在金融風險監控事件流中的應用。
 * 所屬：金融子系統 — adapter 層
 */
@DisplayName("RedisRiskAlertStreamAdapter 整合測試")
class RedisRiskAlertStreamAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisRiskAlertStreamAdapter adapter;

    // 驗證發布風險警報至 Stream 後，消費者群組能正確讀取警報內容
    @Test
    @DisplayName("publishAndConsume_ReturnsPublishedAlert — 發布後消費應取回已發布的警報")
    void publishAndConsume_ReturnsPublishedAlert() {
        // Arrange — publish a risk alert
        RiskAlert alert = new RiskAlert("alert-test-001", "acc-001",
                "SUSPICIOUS_TRANSFER", "Large transfer detected", "HIGH",
                1700000000L);
        adapter.publishAlert(alert);

        // Act — consume alerts from the stream
        List<RiskAlert> consumed = adapter.consumeAlerts(
                "finance-risk-group", "consumer-1", 10);

        // Assert — should contain the published alert
        assertThat(consumed).isNotEmpty();
        assertThat(consumed).anyMatch(a ->
                "alert-test-001".equals(a.getAlertId())
                        && "acc-001".equals(a.getAccountId())
                        && "SUSPICIOUS_TRANSFER".equals(a.getAlertType())
                        && "HIGH".equals(a.getSeverity()));
    }
}
