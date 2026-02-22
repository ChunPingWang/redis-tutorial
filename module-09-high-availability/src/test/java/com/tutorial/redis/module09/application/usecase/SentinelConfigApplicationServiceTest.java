package com.tutorial.redis.module09.application.usecase;

import com.tutorial.redis.module09.domain.model.FailoverEvent;
import com.tutorial.redis.module09.domain.model.SentinelConfig;
import com.tutorial.redis.module09.domain.service.FailoverProcessService;
import com.tutorial.redis.module09.domain.service.SentinelConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Sentinel 配置應用服務單元測試
 * 驗證 SentinelConfigApplicationService 正確協調 Sentinel 配置與故障轉移流程的領域服務。
 * 屬於 Application 層，測試 Sentinel 哨兵推薦配置取得及 Failover 流程描述的委派邏輯。
 */
@DisplayName("SentinelConfigApplicationService 單元測試")
@ExtendWith(MockitoExtension.class)
class SentinelConfigApplicationServiceTest {

    @Mock
    private SentinelConfigService sentinelConfigService;

    @Mock
    private FailoverProcessService failoverProcessService;

    @InjectMocks
    private SentinelConfigApplicationService service;

    // 驗證取得 Sentinel 推薦配置時，應委派給 SentinelConfigService 並回傳正確結果
    @Test
    @DisplayName("getRecommendedConfig_DelegatesToService — 取得推薦配置應委派給 SentinelConfigService")
    void getRecommendedConfig_DelegatesToService() {
        // Arrange
        SentinelConfig expected = new SentinelConfig("mymaster", 2, 30000, 180000, 1);
        when(sentinelConfigService.getRecommendedConfig("mymaster")).thenReturn(expected);

        // Act
        SentinelConfig result = service.getRecommendedConfig("mymaster");

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(sentinelConfigService, times(1)).getRecommendedConfig("mymaster");
    }

    // 驗證描述故障轉移流程時，應委派給 FailoverProcessService 並回傳完整事件列表
    @Test
    @DisplayName("describeFailoverProcess_DelegatesToService — 描述故障轉移流程應委派給 FailoverProcessService")
    void describeFailoverProcess_DelegatesToService() {
        // Arrange
        List<FailoverEvent> expected = List.of(
                new FailoverEvent("SDOWN", "主觀下線", System.currentTimeMillis()),
                new FailoverEvent("ODOWN", "客觀下線", System.currentTimeMillis())
        );
        when(failoverProcessService.describeFailoverProcess()).thenReturn(expected);

        // Act
        List<FailoverEvent> result = service.describeFailoverProcess();

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(failoverProcessService, times(1)).describeFailoverProcess();
    }
}
