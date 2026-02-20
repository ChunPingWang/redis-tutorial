package com.tutorial.redis.module05.application.usecase;

import com.tutorial.redis.module05.domain.model.StockDeductionResult;
import com.tutorial.redis.module05.domain.port.outbound.AtomicStockDeductionPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AtomicStockDeductionService 單元測試")
class AtomicStockDeductionServiceTest {

    @Mock
    private AtomicStockDeductionPort atomicStockDeductionPort;

    @InjectMocks
    private AtomicStockDeductionService service;

    @Test
    @DisplayName("deductStock_DelegatesToPort — 扣減庫存委派給 AtomicStockDeductionPort 執行")
    void deductStock_DelegatesToPort() {
        // Arrange
        when(atomicStockDeductionPort.deductStock("PROD-001", 5))
                .thenReturn(StockDeductionResult.SUCCESS);

        // Act
        StockDeductionResult result = service.deductStock("PROD-001", 5);

        // Assert
        assertThat(result).isEqualTo(StockDeductionResult.SUCCESS);
        verify(atomicStockDeductionPort, times(1)).deductStock("PROD-001", 5);
    }

    @Test
    @DisplayName("initializeStock_CallsSetStock — 初始化庫存呼叫 setStock")
    void initializeStock_CallsSetStock() {
        // Arrange
        doNothing().when(atomicStockDeductionPort).setStock("PROD-001", 100L);

        // Act
        service.initializeStock("PROD-001", 100L);

        // Assert
        verify(atomicStockDeductionPort, times(1)).setStock("PROD-001", 100L);
    }

    @Test
    @DisplayName("getStock_DelegatesToPort — 查詢庫存委派給 AtomicStockDeductionPort 執行")
    void getStock_DelegatesToPort() {
        // Arrange
        when(atomicStockDeductionPort.getStock("PROD-001"))
                .thenReturn(Optional.of(50L));

        // Act
        Optional<Long> result = service.getStock("PROD-001");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(50L);
        verify(atomicStockDeductionPort, times(1)).getStock("PROD-001");
    }
}
