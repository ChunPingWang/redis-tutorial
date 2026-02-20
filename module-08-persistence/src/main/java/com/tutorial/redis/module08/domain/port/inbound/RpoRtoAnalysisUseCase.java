package com.tutorial.redis.module08.domain.port.inbound;

import com.tutorial.redis.module08.domain.model.RpoRtoAnalysis;

import java.util.List;

/**
 * Inbound port: RPO/RTO analysis for Redis persistence strategies.
 *
 * <p>Provides qualitative analysis of Recovery Point Objective (RPO) and
 * Recovery Time Objective (RTO) trade-offs for each persistence strategy,
 * helping users choose the right configuration for their use case.</p>
 */
public interface RpoRtoAnalysisUseCase {

    /**
     * Analyzes all known persistence strategies and returns their
     * RPO/RTO characteristics.
     *
     * @return a list of {@link RpoRtoAnalysis} for every supported strategy
     */
    List<RpoRtoAnalysis> analyzeAllStrategies();

    /**
     * Analyzes a specific persistence strategy by name.
     *
     * @param strategy the strategy name (e.g. "rdb", "aof-always", "aof-everysec", "hybrid")
     * @return the {@link RpoRtoAnalysis} for the requested strategy
     * @throws IllegalArgumentException if the strategy name is not recognized
     */
    RpoRtoAnalysis analyzeStrategy(String strategy);
}
