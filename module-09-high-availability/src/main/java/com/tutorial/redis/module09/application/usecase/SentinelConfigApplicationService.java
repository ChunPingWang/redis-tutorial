package com.tutorial.redis.module09.application.usecase;

import com.tutorial.redis.module09.domain.model.FailoverEvent;
import com.tutorial.redis.module09.domain.model.SentinelConfig;
import com.tutorial.redis.module09.domain.port.inbound.SentinelConfigUseCase;
import com.tutorial.redis.module09.domain.service.FailoverProcessService;
import com.tutorial.redis.module09.domain.service.SentinelConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service for Sentinel configuration recommendations and
 * failover process descriptions.
 *
 * <p>Implements the {@link SentinelConfigUseCase} inbound port by delegating
 * to the {@link SentinelConfigService} domain service for configuration
 * recommendations and to the {@link FailoverProcessService} domain service
 * for the failover event sequence.</p>
 *
 * <p>This thin application layer exists to maintain the hexagonal architecture
 * boundary between the REST controller (inbound adapter) and the pure
 * domain services.</p>
 */
@Service
public class SentinelConfigApplicationService implements SentinelConfigUseCase {

    private static final Logger log = LoggerFactory.getLogger(SentinelConfigApplicationService.class);

    private final SentinelConfigService sentinelConfigService;
    private final FailoverProcessService failoverProcessService;

    public SentinelConfigApplicationService(SentinelConfigService sentinelConfigService,
                                            FailoverProcessService failoverProcessService) {
        this.sentinelConfigService = sentinelConfigService;
        this.failoverProcessService = failoverProcessService;
    }

    /**
     * Returns the recommended Sentinel configuration for the given master name.
     *
     * @param masterName the logical name of the master to monitor
     * @return a {@link SentinelConfig} with recommended settings
     */
    @Override
    public SentinelConfig getRecommendedConfig(String masterName) {
        log.debug("Retrieving recommended Sentinel config for master '{}'", masterName);
        return sentinelConfigService.getRecommendedConfig(masterName);
    }

    /**
     * Describes the Sentinel failover process as a sequence of events.
     *
     * @return a list of {@link FailoverEvent} describing each step of the failover
     */
    @Override
    public List<FailoverEvent> describeFailoverProcess() {
        log.debug("Describing Sentinel failover process");
        return failoverProcessService.describeFailoverProcess();
    }
}
