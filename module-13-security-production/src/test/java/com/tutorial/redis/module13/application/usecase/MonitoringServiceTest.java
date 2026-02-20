package com.tutorial.redis.module13.application.usecase;

import com.tutorial.redis.module13.domain.model.EvictionPolicy;
import com.tutorial.redis.module13.domain.model.MemoryInfo;
import com.tutorial.redis.module13.domain.model.ServerMetrics;
import com.tutorial.redis.module13.domain.port.outbound.MonitoringPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MonitoringService 單元測試")
@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private MonitoringPort port;

    @InjectMocks
    private MonitoringService service;

    @Test
    @DisplayName("getMemoryInfo_DelegatesToPort — 取得記憶體資訊應委派給 MonitoringPort")
    void getMemoryInfo_DelegatesToPort() {
        // Arrange — stub the port to return a MemoryInfo
        MemoryInfo expectedInfo = new MemoryInfo(1024000L, 2048000L,
                EvictionPolicy.NOEVICTION, 50.0, 1500000L);
        when(port.getMemoryInfo()).thenReturn(expectedInfo);

        // Act
        MemoryInfo result = service.getMemoryInfo();

        // Assert — verify delegation and correct result
        verify(port).getMemoryInfo();
        assertThat(result).isSameAs(expectedInfo);
        assertThat(result.getUsedMemory()).isEqualTo(1024000L);
    }

    @Test
    @DisplayName("getServerMetrics_DelegatesToPort — 取得服務指標應委派給 MonitoringPort")
    void getServerMetrics_DelegatesToPort() {
        // Arrange — stub the port to return ServerMetrics
        ServerMetrics expectedMetrics = new ServerMetrics(
                10L, 5000L, 200L, 0.96, 150L, 100000L, 86400L);
        when(port.getServerMetrics()).thenReturn(expectedMetrics);

        // Act
        ServerMetrics result = service.getServerMetrics();

        // Assert — verify delegation and correct result
        verify(port).getServerMetrics();
        assertThat(result).isSameAs(expectedMetrics);
        assertThat(result.getUptimeInSeconds()).isEqualTo(86400L);
    }
}
