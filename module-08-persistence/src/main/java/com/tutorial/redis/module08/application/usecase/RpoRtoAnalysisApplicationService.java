package com.tutorial.redis.module08.application.usecase;

import com.tutorial.redis.module08.domain.model.RpoRtoAnalysis;
import com.tutorial.redis.module08.domain.port.inbound.RpoRtoAnalysisUseCase;
import com.tutorial.redis.module08.domain.service.RpoRtoAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service for RPO/RTO analysis of Redis persistence strategies.
 *
 * <p>Implements the {@link RpoRtoAnalysisUseCase} inbound port by delegating
 * to the {@link RpoRtoAnalysisService} domain service. This thin application
 * layer exists to maintain the hexagonal architecture boundary between
 * the REST controller (inbound adapter) and the pure domain service.</p>
 */
@Service
public class RpoRtoAnalysisApplicationService implements RpoRtoAnalysisUseCase {

    private static final Logger log = LoggerFactory.getLogger(RpoRtoAnalysisApplicationService.class);

    private final RpoRtoAnalysisService rpoRtoAnalysisService;

    public RpoRtoAnalysisApplicationService(RpoRtoAnalysisService rpoRtoAnalysisService) {
        this.rpoRtoAnalysisService = rpoRtoAnalysisService;
    }

    /**
     * Analyzes all known persistence strategies and returns their
     * RPO/RTO characteristics.
     *
     * @return a list of {@link RpoRtoAnalysis} for every supported strategy
     */
    @Override
    public List<RpoRtoAnalysis> analyzeAllStrategies() {
        log.debug("Analyzing all persistence strategies for RPO/RTO");
        return rpoRtoAnalysisService.analyzeAllStrategies();
    }

    /**
     * Analyzes a specific persistence strategy by name.
     *
     * @param strategy the strategy name (e.g. "rdb", "aof-always", "aof-everysec", "hybrid")
     * @return the {@link RpoRtoAnalysis} for the requested strategy
     * @throws IllegalArgumentException if the strategy name is not recognized
     */
    @Override
    public RpoRtoAnalysis analyzeStrategy(String strategy) {
        log.debug("Analyzing persistence strategy '{}' for RPO/RTO", strategy);
        return rpoRtoAnalysisService.analyzeStrategy(strategy);
    }
}
