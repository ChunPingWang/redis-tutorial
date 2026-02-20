package com.tutorial.redis.module09.application.usecase;

import com.tutorial.redis.module09.domain.model.FailoverEvent;
import com.tutorial.redis.module09.domain.port.inbound.FailoverSimulationUseCase;
import com.tutorial.redis.module09.domain.port.outbound.FailoverSimulationPort;
import com.tutorial.redis.module09.domain.service.FailoverProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service for simulating and verifying failover scenarios.
 *
 * <p>Implements the {@link FailoverSimulationUseCase} inbound port by
 * delegating to the {@link FailoverProcessService} domain service for
 * generating the educational failover sequence and to the
 * {@link FailoverSimulationPort} outbound port for data integrity checks.</p>
 *
 * <p>This thin application layer exists to maintain the hexagonal architecture
 * boundary between the REST controller (inbound adapter) and the domain
 * services / Redis adapter (outbound).</p>
 */
@Service
public class FailoverSimulationService implements FailoverSimulationUseCase {

    private static final Logger log = LoggerFactory.getLogger(FailoverSimulationService.class);

    private final FailoverSimulationPort failoverSimulationPort;
    private final FailoverProcessService failoverProcessService;

    public FailoverSimulationService(FailoverSimulationPort failoverSimulationPort,
                                     FailoverProcessService failoverProcessService) {
        this.failoverSimulationPort = failoverSimulationPort;
        this.failoverProcessService = failoverProcessService;
    }

    /**
     * Simulates the Sentinel failover process and returns an ordered
     * sequence of failover events describing each step.
     *
     * <p>Delegates to {@link FailoverProcessService#describeFailoverProcess()}
     * which generates the educational event sequence.</p>
     *
     * @return a list of {@link FailoverEvent} in chronological order
     */
    @Override
    public List<FailoverEvent> simulateFailoverSequence() {
        log.debug("Simulating failover sequence");
        return failoverProcessService.describeFailoverProcess();
    }

    /**
     * Verifies data integrity after a simulated failover by checking how
     * many keys with the given prefix survived the transition.
     *
     * @param keyPrefix     the prefix of keys to verify
     * @param expectedCount the expected total number of keys
     * @return the number of keys that were successfully read back
     */
    @Override
    public int verifyDataAfterFailover(String keyPrefix, int expectedCount) {
        log.info("Verifying data after failover: prefix='{}', expectedCount={}", keyPrefix, expectedCount);
        return failoverSimulationPort.verifyDataIntegrity(keyPrefix, expectedCount);
    }
}
