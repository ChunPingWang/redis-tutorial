package com.tutorial.redis.module08.domain.service;

import com.tutorial.redis.module08.domain.model.RpoRtoAnalysis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 測試 RpoRtoAnalysisService 的 RPO/RTO 分析邏輯。
 * 驗證對 RDB、AOF、Hybrid、None 四種持久化策略的恢復點目標與恢復時間目標分析。
 * 屬於 Domain 層（領域服務），為純粹的業務邏輯單元測試。
 */
@DisplayName("RpoRtoAnalysisService 領域服務測試")
class RpoRtoAnalysisServiceTest {

    private final RpoRtoAnalysisService service = new RpoRtoAnalysisService();

    // 驗證分析所有持久化策略時，應回傳 RDB、AOF、Hybrid、None 共 4 種
    @Test
    @DisplayName("analyzeAllStrategies_ReturnsFourStrategies — 分析所有策略應回傳 4 種")
    void analyzeAllStrategies_ReturnsFourStrategies() {
        // Act
        List<RpoRtoAnalysis> strategies = service.analyzeAllStrategies();

        // Assert
        assertThat(strategies).hasSize(4);
    }

    // 驗證 RDB 策略的 RPO 描述包含「分鐘」，因 RDB 為週期性快照
    @Test
    @DisplayName("analyzeStrategy_Rdb_ReturnsCorrectAnalysis — 分析 RDB 策略，RPO 應為分鐘級")
    void analyzeStrategy_Rdb_ReturnsCorrectAnalysis() {
        // Act
        RpoRtoAnalysis result = service.analyzeStrategy("rdb");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStrategy()).isEqualTo("rdb");
        assertThat(result.getRpoDescription()).contains("分鐘");
    }

    // 驗證 Hybrid 混合持久化策略的推薦場景包含「推薦」字樣
    @Test
    @DisplayName("analyzeStrategy_Hybrid_IsRecommended — 分析 Hybrid 策略，應為推薦預設")
    void analyzeStrategy_Hybrid_IsRecommended() {
        // Act
        RpoRtoAnalysis result = service.analyzeStrategy("hybrid");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStrategy()).isEqualTo("hybrid");
        assertThat(result.getRecommendedScenario()).contains("推薦");
    }

    // 驗證傳入未知的策略名稱時，應拋出 IllegalArgumentException
    @Test
    @DisplayName("analyzeStrategy_Unknown_ThrowsIAE — 未知策略應拋出 IllegalArgumentException")
    void analyzeStrategy_Unknown_ThrowsIAE() {
        // Act & Assert
        assertThatThrownBy(() -> service.analyzeStrategy("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
