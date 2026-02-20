package com.tutorial.redis.module08.domain.service;

import com.tutorial.redis.module08.domain.model.RpoRtoAnalysis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RpoRtoAnalysisService 領域服務測試")
class RpoRtoAnalysisServiceTest {

    private final RpoRtoAnalysisService service = new RpoRtoAnalysisService();

    @Test
    @DisplayName("analyzeAllStrategies_ReturnsFourStrategies — 分析所有策略應回傳 4 種")
    void analyzeAllStrategies_ReturnsFourStrategies() {
        // Act
        List<RpoRtoAnalysis> strategies = service.analyzeAllStrategies();

        // Assert
        assertThat(strategies).hasSize(4);
    }

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

    @Test
    @DisplayName("analyzeStrategy_Unknown_ThrowsIAE — 未知策略應拋出 IllegalArgumentException")
    void analyzeStrategy_Unknown_ThrowsIAE() {
        // Act & Assert
        assertThatThrownBy(() -> service.analyzeStrategy("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
