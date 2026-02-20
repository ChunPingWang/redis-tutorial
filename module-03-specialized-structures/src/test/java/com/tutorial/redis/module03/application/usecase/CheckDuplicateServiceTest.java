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

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckDuplicateService 單元測試")
class CheckDuplicateServiceTest {

    @Mock
    private BloomFilterPort bloomFilterPort;

    @InjectMocks
    private CheckDuplicateService service;

    @Test
    @DisplayName("initializeFilter_DelegatesToPort — 委派至 Port 的 createFilter 方法")
    void initializeFilter_DelegatesToPort() {
        service.initializeFilter("dedup-filter", 0.01, 10000);

        verify(bloomFilterPort).createFilter("dedup-filter", 0.01, 10000);
    }

    @Test
    @DisplayName("markAsProcessed_DelegatesToPort — 委派至 Port 的 add 方法")
    void markAsProcessed_DelegatesToPort() {
        when(bloomFilterPort.add("dedup-filter", "ORDER-001")).thenReturn(true);

        boolean result = service.markAsProcessed("dedup-filter", "ORDER-001");

        assertThat(result).isTrue();
        verify(bloomFilterPort).add("dedup-filter", "ORDER-001");
    }

    @Test
    @DisplayName("mightBeProcessed_DelegatesToPort — 委派至 Port 的 mightContain 方法")
    void mightBeProcessed_DelegatesToPort() {
        when(bloomFilterPort.mightContain("dedup-filter", "ORDER-001")).thenReturn(true);

        boolean result = service.mightBeProcessed("dedup-filter", "ORDER-001");

        assertThat(result).isTrue();
        verify(bloomFilterPort).mightContain("dedup-filter", "ORDER-001");
    }
}
