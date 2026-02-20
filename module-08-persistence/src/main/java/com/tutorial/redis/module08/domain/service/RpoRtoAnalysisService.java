package com.tutorial.redis.module08.domain.service;

import com.tutorial.redis.module08.domain.model.RpoRtoAnalysis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure domain service that provides pre-defined RPO/RTO analysis
 * for each Redis persistence strategy.
 *
 * <p>This service has zero framework dependencies — it operates entirely
 * on domain knowledge about Redis persistence trade-offs. Each strategy
 * is characterised by its Recovery Point Objective (data loss window),
 * Recovery Time Objective (restart duration), performance overhead,
 * and recommended scenario.</p>
 *
 * <p>Supported strategies:</p>
 * <ul>
 *   <li>{@code rdb} — periodic snapshots, minute-level RPO, fast RTO</li>
 *   <li>{@code aof-always} — fsync every write, zero RPO, slow RTO</li>
 *   <li>{@code aof-everysec} — fsync every second, ~1s RPO, slow RTO</li>
 *   <li>{@code hybrid} — RDB preamble + AOF tail, ~1s RPO, medium RTO</li>
 * </ul>
 */
public class RpoRtoAnalysisService {

    private final Map<String, RpoRtoAnalysis> analyses;

    public RpoRtoAnalysisService() {
        this.analyses = new LinkedHashMap<>();
        initializeAnalyses();
    }

    /**
     * Populates the pre-defined RPO/RTO analyses for all supported strategies.
     */
    private void initializeAnalyses() {
        analyses.put("rdb", new RpoRtoAnalysis(
                "rdb",
                "分鐘級",
                "快 (載入 dump)",
                "低",
                "可容忍資料遺失的快取場景"
        ));

        analyses.put("aof-always", new RpoRtoAnalysis(
                "aof-always",
                "0 (每筆 fsync)",
                "慢 (重播 log)",
                "高",
                "金融交易零遺失"
        ));

        analyses.put("aof-everysec", new RpoRtoAnalysis(
                "aof-everysec",
                "~1 秒",
                "慢 (重播 log)",
                "中",
                "一般業務"
        ));

        analyses.put("hybrid", new RpoRtoAnalysis(
                "hybrid",
                "~1 秒",
                "中 (RDB 前導 + AOF 尾段)",
                "中",
                "推薦預設"
        ));
    }

    /**
     * Returns RPO/RTO analyses for all supported persistence strategies.
     *
     * @return an unmodifiable list of all {@link RpoRtoAnalysis} entries
     */
    public List<RpoRtoAnalysis> analyzeAllStrategies() {
        return List.copyOf(analyses.values());
    }

    /**
     * Returns the RPO/RTO analysis for a specific persistence strategy.
     *
     * @param strategy the strategy name (e.g. "rdb", "aof-always", "aof-everysec", "hybrid")
     * @return the corresponding {@link RpoRtoAnalysis}
     * @throws IllegalArgumentException if the strategy name is not recognized
     */
    public RpoRtoAnalysis analyzeStrategy(String strategy) {
        Objects.requireNonNull(strategy, "strategy must not be null");
        RpoRtoAnalysis analysis = analyses.get(strategy);
        if (analysis == null) {
            throw new IllegalArgumentException(
                    "Unknown persistence strategy: '%s'. Supported: %s".formatted(
                            strategy, analyses.keySet()));
        }
        return analysis;
    }
}
