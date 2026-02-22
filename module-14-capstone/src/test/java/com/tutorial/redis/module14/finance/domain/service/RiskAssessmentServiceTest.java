package com.tutorial.redis.module14.finance.domain.service;

import com.tutorial.redis.module14.finance.domain.model.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RiskAssessmentService 領域服務單元測試類別。
 * 驗證交易風險評估的純領域邏輯：高風險判定與嚴重等級分類。
 * 展示六角形架構中 domain 層業務規則的獨立測試，不依賴 Redis。
 * 所屬：金融子系統 — domain 層
 */
@DisplayName("RiskAssessmentService 領域服務測試")
class RiskAssessmentServiceTest {

    private final RiskAssessmentService service = new RiskAssessmentService();

    // 驗證交易金額超過 10000 閾值時，應判定為高風險交易
    @Test
    @DisplayName("isHighRiskTransaction_WhenAboveThreshold_ReturnsTrue — 金額超過閾值應判定為高風險")
    void isHighRiskTransaction_WhenAboveThreshold_ReturnsTrue() {
        // Arrange — create a transaction with amount > 10000
        Transaction tx = new Transaction("tx-001", "acc-001", "acc-002",
                15000.0, "USD", System.currentTimeMillis(), "PENDING");

        // Act
        boolean result = service.isHighRiskTransaction(tx);

        // Assert
        assertThat(result).isTrue();
    }

    // 驗證交易金額低於 10000 閾值時，應判定為非高風險交易
    @Test
    @DisplayName("isHighRiskTransaction_WhenBelowThreshold_ReturnsFalse — 金額低於閾值應判定為非高風險")
    void isHighRiskTransaction_WhenBelowThreshold_ReturnsFalse() {
        // Arrange — create a transaction with amount <= 10000
        Transaction tx = new Transaction("tx-002", "acc-001", "acc-002",
                5000.0, "USD", System.currentTimeMillis(), "PENDING");

        // Act
        boolean result = service.isHighRiskTransaction(tx);

        // Assert
        assertThat(result).isFalse();
    }

    // 驗證根據金額區間回傳正確的嚴重等級（LOW/MEDIUM/HIGH/CRITICAL）
    @Test
    @DisplayName("determineSeverity_ReturnsCorrectLevel — 根據金額回傳正確的嚴重等級")
    void determineSeverity_ReturnsCorrectLevel() {
        // Assert — LOW: amount <= 1000
        assertThat(service.determineSeverity(500.0)).isEqualTo("LOW");
        assertThat(service.determineSeverity(1000.0)).isEqualTo("LOW");

        // Assert — MEDIUM: 1000 < amount <= 5000
        assertThat(service.determineSeverity(1001.0)).isEqualTo("MEDIUM");
        assertThat(service.determineSeverity(5000.0)).isEqualTo("MEDIUM");

        // Assert — HIGH: 5000 < amount <= 10000
        assertThat(service.determineSeverity(5001.0)).isEqualTo("HIGH");
        assertThat(service.determineSeverity(10000.0)).isEqualTo("HIGH");

        // Assert — CRITICAL: amount > 10000
        assertThat(service.determineSeverity(10001.0)).isEqualTo("CRITICAL");
        assertThat(service.determineSeverity(50000.0)).isEqualTo("CRITICAL");
    }
}
