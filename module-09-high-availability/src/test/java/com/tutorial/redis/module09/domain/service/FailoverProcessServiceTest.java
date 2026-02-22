package com.tutorial.redis.module09.domain.service;

import com.tutorial.redis.module09.domain.model.FailoverEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 故障轉移流程領域服務測試
 * 驗證 Sentinel 故障轉移的完整流程步驟（SDOWN -> ODOWN -> 選舉 -> 切換 -> 完成）。
 * 屬於 Domain 層（領域服務），展示 Redis Sentinel Failover 的五階段事件模型。
 */
@DisplayName("FailoverProcessService 領域服務測試")
class FailoverProcessServiceTest {

    private final FailoverProcessService service = new FailoverProcessService();

    // 驗證故障轉移流程應包含完整的 5 個步驟事件
    @Test
    @DisplayName("describeFailoverProcess_ReturnsFiveSteps — 故障轉移流程應回傳 5 個步驟")
    void describeFailoverProcess_ReturnsFiveSteps() {
        // Act
        List<FailoverEvent> events = service.describeFailoverProcess();

        // Assert
        assertThat(events).hasSize(5);
    }

    // 驗證故障轉移流程的第一步應為 SDOWN（Sentinel 主觀判定節點下線）
    @Test
    @DisplayName("describeFailoverProcess_StartsWithSdown — 第一步應為 SDOWN 主觀下線")
    void describeFailoverProcess_StartsWithSdown() {
        // Act
        List<FailoverEvent> events = service.describeFailoverProcess();

        // Assert
        assertThat(events.get(0).getEventType()).isEqualTo("SDOWN");
    }

    // 驗證故障轉移流程的最後一步應為 FAILOVER_END（故障轉移完成）
    @Test
    @DisplayName("describeFailoverProcess_EndsWithFailoverEnd — 最後一步應為故障轉移完成")
    void describeFailoverProcess_EndsWithFailoverEnd() {
        // Act
        List<FailoverEvent> events = service.describeFailoverProcess();

        // Assert
        assertThat(events.get(4).getEventType()).isEqualTo("FAILOVER_END");
    }
}
