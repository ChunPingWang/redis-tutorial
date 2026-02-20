package com.tutorial.redis.module08.adapter.inbound.rest;

import com.tutorial.redis.module08.domain.model.PersistenceStatus;
import com.tutorial.redis.module08.domain.model.RecoveryResult;
import com.tutorial.redis.module08.domain.model.RpoRtoAnalysis;
import com.tutorial.redis.module08.domain.port.inbound.DataRecoveryUseCase;
import com.tutorial.redis.module08.domain.port.inbound.PersistenceInfoUseCase;
import com.tutorial.redis.module08.domain.port.inbound.RpoRtoAnalysisUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing endpoints for Redis persistence operations:
 * <ul>
 *   <li>Querying current persistence status</li>
 *   <li>Triggering RDB snapshots and AOF rewrites</li>
 *   <li>Simulating data recovery scenarios</li>
 *   <li>Analysing RPO/RTO trade-offs for each persistence strategy</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/persistence")
public class PersistenceController {

    private final PersistenceInfoUseCase persistenceInfoUseCase;
    private final DataRecoveryUseCase dataRecoveryUseCase;
    private final RpoRtoAnalysisUseCase rpoRtoAnalysisUseCase;

    public PersistenceController(PersistenceInfoUseCase persistenceInfoUseCase,
                                 DataRecoveryUseCase dataRecoveryUseCase,
                                 RpoRtoAnalysisUseCase rpoRtoAnalysisUseCase) {
        this.persistenceInfoUseCase = persistenceInfoUseCase;
        this.dataRecoveryUseCase = dataRecoveryUseCase;
        this.rpoRtoAnalysisUseCase = rpoRtoAnalysisUseCase;
    }

    /**
     * Retrieves the current persistence status from the connected Redis instance.
     *
     * @return the current {@link PersistenceStatus} including RDB/AOF state
     */
    @GetMapping("/status")
    public PersistenceStatus getStatus() {
        return persistenceInfoUseCase.getPersistenceStatus();
    }

    /**
     * Triggers an asynchronous RDB background save (BGSAVE).
     * Redis forks a child process to create a point-in-time snapshot.
     */
    @PostMapping("/bgsave")
    public void triggerBgsave() {
        persistenceInfoUseCase.triggerRdbSnapshot();
    }

    /**
     * Triggers an asynchronous AOF background rewrite (BGREWRITEAOF).
     * Redis forks a child process to compact the append-only file.
     */
    @PostMapping("/bgrewriteaof")
    public void triggerBgrewriteaof() {
        persistenceInfoUseCase.triggerAofRewrite();
    }

    /**
     * Simulates a data recovery scenario by writing test keys and measuring
     * how many are recoverable and how long the count operation takes.
     *
     * @param keyPrefix the prefix used for test keys
     * @param keyCount  the number of test keys to write
     * @return a {@link RecoveryResult} with recovery metrics
     */
    @PostMapping("/simulate-recovery")
    public RecoveryResult simulateRecovery(@RequestParam String keyPrefix,
                                           @RequestParam int keyCount) {
        return dataRecoveryUseCase.simulateRecovery(keyPrefix, keyCount);
    }

    /**
     * Analyzes RPO/RTO trade-offs for all known persistence strategies.
     *
     * @return a list of {@link RpoRtoAnalysis} for every supported strategy
     */
    @GetMapping("/rpo-rto")
    public List<RpoRtoAnalysis> analyzeAll() {
        return rpoRtoAnalysisUseCase.analyzeAllStrategies();
    }

    /**
     * Analyzes RPO/RTO trade-offs for a specific persistence strategy.
     *
     * @param strategy the strategy name (e.g. "rdb", "aof-always", "aof-everysec", "hybrid")
     * @return the {@link RpoRtoAnalysis} for the requested strategy
     */
    @GetMapping("/rpo-rto/{strategy}")
    public RpoRtoAnalysis analyzeStrategy(@PathVariable String strategy) {
        return rpoRtoAnalysisUseCase.analyzeStrategy(strategy);
    }
}
