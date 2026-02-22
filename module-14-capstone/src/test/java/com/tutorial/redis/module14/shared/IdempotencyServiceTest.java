package com.tutorial.redis.module14.shared;

import com.tutorial.redis.module14.shared.application.usecase.IdempotencyService;
import com.tutorial.redis.module14.shared.domain.port.outbound.IdempotencyPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IdempotencyService 應用層單元測試類別。
 * 驗證冪等性服務的檢查並設定與結果查詢邏輯，包含 Key 前綴組裝。
 * 展示使用 Redis SET NX EX 實現操作冪等性保證的應用層封裝。
 * 所屬：共用分散式模式 — shared 層
 */
@DisplayName("IdempotencyService 單元測試")
class IdempotencyServiceTest {

    private IdempotencyPort idempotencyPort;
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyPort = Mockito.mock(IdempotencyPort.class);
        idempotencyService = new IdempotencyService(idempotencyPort);
    }

    // 驗證檢查並設定冪等鍵時，自動加上 idempotency: 前綴並委派給 Port
    @Test
    @DisplayName("checkAndSet_DelegatesToPort — 檢查並設定應委派給 Port 並加上 idempotency: 前綴")
    void checkAndSet_DelegatesToPort() {
        // Arrange
        when(idempotencyPort.setIfAbsent("idempotency:pay-001", "success", 3600))
                .thenReturn(true);

        // Act
        boolean result = idempotencyService.checkAndSet("pay-001", "success", 3600);

        // Assert
        assertThat(result).isTrue();
        verify(idempotencyPort).setIfAbsent("idempotency:pay-001", "success", 3600);
    }

    // 驗證取得冪等結果時，自動加上 idempotency: 前綴並委派給 Port
    @Test
    @DisplayName("getResult_DelegatesToPort — 取得結果應委派給 Port 並加上 idempotency: 前綴")
    void getResult_DelegatesToPort() {
        // Arrange
        when(idempotencyPort.get("idempotency:pay-001")).thenReturn("success");

        // Act
        String result = idempotencyService.getResult("pay-001");

        // Assert
        assertThat(result).isEqualTo("success");
        verify(idempotencyPort).get("idempotency:pay-001");
    }
}
