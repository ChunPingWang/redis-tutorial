package com.tutorial.redis.module14.finance.application.usecase;

import com.tutorial.redis.module14.finance.domain.model.RiskAlert;
import com.tutorial.redis.module14.finance.domain.port.outbound.FraudDetectionPort;
import com.tutorial.redis.module14.finance.domain.port.outbound.RiskAlertStreamPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RiskAlertService 應用層單元測試類別。
 * 驗證詐欺偵測（Bloom Filter）與風險警報發布（Stream）的業務邏輯。
 * 展示 Bloom Filter 搭配 Redis Stream 實現風險監控管線。
 * 所屬：金融子系統 — application 層
 */
@DisplayName("RiskAlertService 單元測試")
@ExtendWith(MockitoExtension.class)
class RiskAlertServiceTest {

    @Mock
    private FraudDetectionPort fraudDetectionPort;

    @Mock
    private RiskAlertStreamPort riskAlertStreamPort;

    @InjectMocks
    private RiskAlertService service;

    // 驗證檢查詐欺重複時，正確委派給 FraudDetectionPort（Bloom Filter）
    @Test
    @DisplayName("checkFraudDuplicate_DelegatesToBloomFilter — 檢查詐欺重複應委派給 Bloom 過濾器")
    void checkFraudDuplicate_DelegatesToBloomFilter() {
        // Arrange
        when(fraudDetectionPort.mightExist("tx-001")).thenReturn(true);

        // Act
        boolean result = service.checkFraudDuplicate("tx-001");

        // Assert
        verify(fraudDetectionPort).mightExist("tx-001");
        assertThat(result).isTrue();
    }

    // 驗證發布風險警報時，正確委派給 RiskAlertStreamPort 寫入 Stream
    @Test
    @DisplayName("publishRiskAlert_DelegatesToStream — 發布風險警報應委派給 Stream")
    void publishRiskAlert_DelegatesToStream() {
        // Arrange
        RiskAlert alert = new RiskAlert("alert-001", "acc-001", "SUSPICIOUS_TRANSFER",
                "Large transfer detected", "HIGH", System.currentTimeMillis());

        // Act
        service.publishRiskAlert(alert);

        // Assert
        verify(riskAlertStreamPort).publishAlert(alert);
    }
}
