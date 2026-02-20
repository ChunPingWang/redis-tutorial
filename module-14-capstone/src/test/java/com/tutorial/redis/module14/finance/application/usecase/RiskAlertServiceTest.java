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

@DisplayName("RiskAlertService 單元測試")
@ExtendWith(MockitoExtension.class)
class RiskAlertServiceTest {

    @Mock
    private FraudDetectionPort fraudDetectionPort;

    @Mock
    private RiskAlertStreamPort riskAlertStreamPort;

    @InjectMocks
    private RiskAlertService service;

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
