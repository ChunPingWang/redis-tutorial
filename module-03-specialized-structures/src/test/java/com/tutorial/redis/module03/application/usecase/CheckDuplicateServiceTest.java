package com.tutorial.redis.module03.application.usecase;

import com.tutorial.redis.module03.domain.port.outbound.BloomFilterPort;
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
 * 重複檢查服務單元測試
 * 驗證 CheckDuplicateService 正確委派 Bloom Filter 相關操作至 BloomFilterPort
 * 使用 Mockito 隔離外部依賴，屬於 Application 層（使用案例）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CheckDuplicateService 單元測試")
class CheckDuplicateServiceTest {

    @Mock
    private BloomFilterPort bloomFilterPort;

    @InjectMocks
    private CheckDuplicateService service;

    // 驗證初始化過濾器時正確委派至 BloomFilterPort.createFilter
    @Test
    @DisplayName("initializeFilter_DelegatesToPort — 委派至 Port 的 createFilter 方法")
    void initializeFilter_DelegatesToPort() {
        service.initializeFilter("dedup-filter", 0.01, 10000);

        verify(bloomFilterPort).createFilter("dedup-filter", 0.01, 10000);
    }

    // 驗證標記已處理時正確委派至 BloomFilterPort.add 並回傳結果
    @Test
    @DisplayName("markAsProcessed_DelegatesToPort — 委派至 Port 的 add 方法")
    void markAsProcessed_DelegatesToPort() {
        when(bloomFilterPort.add("dedup-filter", "ORDER-001")).thenReturn(true);

        boolean result = service.markAsProcessed("dedup-filter", "ORDER-001");

        assertThat(result).isTrue();
        verify(bloomFilterPort).add("dedup-filter", "ORDER-001");
    }

    // 驗證查詢是否已處理時正確委派至 BloomFilterPort.mightContain 並回傳結果
    @Test
    @DisplayName("mightBeProcessed_DelegatesToPort — 委派至 Port 的 mightContain 方法")
    void mightBeProcessed_DelegatesToPort() {
        when(bloomFilterPort.mightContain("dedup-filter", "ORDER-001")).thenReturn(true);

        boolean result = service.mightBeProcessed("dedup-filter", "ORDER-001");

        assertThat(result).isTrue();
        verify(bloomFilterPort).mightContain("dedup-filter", "ORDER-001");
    }
}
