package com.tutorial.redis.module02.application.usecase;

import com.tutorial.redis.module02.domain.port.outbound.StockLevelPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManageStockService 單元測試")
class ManageStockServiceTest {

    @Mock
    private StockLevelPort stockLevelPort;

    @InjectMocks
    private ManageStockService service;

    @Test
    @DisplayName("restockProduct_DelegatesToPort_Increment — 委派至 Port 的 increment 方法")
    void restockProduct_DelegatesToPort_Increment() {
        when(stockLevelPort.increment("PROD-001", 10)).thenReturn(110L);

        long result = service.restockProduct("PROD-001", 10);

        assertThat(result).isEqualTo(110L);
        verify(stockLevelPort).increment("PROD-001", 10);
    }

    @Test
    @DisplayName("purchaseProduct_DelegatesToPort_Decrement — 委派至 Port 的 decrement 方法")
    void purchaseProduct_DelegatesToPort_Decrement() {
        when(stockLevelPort.decrement("PROD-001", 5)).thenReturn(95L);

        long result = service.purchaseProduct("PROD-001", 5);

        assertThat(result).isEqualTo(95L);
        verify(stockLevelPort).decrement("PROD-001", 5);
    }

    @Test
    @DisplayName("getStockLevel_DelegatesToPort — 委派至 Port 的 getLevel 方法")
    void getStockLevel_DelegatesToPort() {
        when(stockLevelPort.getLevel("PROD-001")).thenReturn(OptionalLong.of(50));

        OptionalLong result = service.getStockLevel("PROD-001");

        assertThat(result).isPresent();
        assertThat(result.getAsLong()).isEqualTo(50);
        verify(stockLevelPort).getLevel("PROD-001");
    }

    @Test
    @DisplayName("getStockLevels_DelegatesToPort — 委派至 Port 的 batchGetLevels 方法")
    void getStockLevels_DelegatesToPort() {
        List<String> ids = List.of("P-001", "P-002");
        Map<String, Long> expected = Map.of("P-001", 10L, "P-002", 20L);
        when(stockLevelPort.batchGetLevels(ids)).thenReturn(expected);

        Map<String, Long> result = service.getStockLevels(ids);

        assertThat(result).isEqualTo(expected);
        verify(stockLevelPort).batchGetLevels(ids);
    }
}
