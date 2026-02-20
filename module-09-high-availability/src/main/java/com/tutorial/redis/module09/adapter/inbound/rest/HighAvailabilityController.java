package com.tutorial.redis.module09.adapter.inbound.rest;

import com.tutorial.redis.module09.domain.model.FailoverEvent;
import com.tutorial.redis.module09.domain.model.ReadWriteStrategy;
import com.tutorial.redis.module09.domain.model.ReplicationInfo;
import com.tutorial.redis.module09.domain.model.SentinelConfig;
import com.tutorial.redis.module09.domain.port.inbound.FailoverSimulationUseCase;
import com.tutorial.redis.module09.domain.port.inbound.ReplicationInfoUseCase;
import com.tutorial.redis.module09.domain.port.inbound.SentinelConfigUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing endpoints for Redis high availability operations:
 * <ul>
 *   <li>Querying replication status from the current Redis instance</li>
 *   <li>Listing read-write splitting strategies for master-replica topologies</li>
 *   <li>Simulating Sentinel failover event sequences</li>
 *   <li>Verifying data integrity after a simulated failover</li>
 *   <li>Retrieving recommended Sentinel configuration</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ha")
public class HighAvailabilityController {

    private final ReplicationInfoUseCase replicationInfoUseCase;
    private final FailoverSimulationUseCase failoverSimulationUseCase;
    private final SentinelConfigUseCase sentinelConfigUseCase;

    public HighAvailabilityController(ReplicationInfoUseCase replicationInfoUseCase,
                                      FailoverSimulationUseCase failoverSimulationUseCase,
                                      SentinelConfigUseCase sentinelConfigUseCase) {
        this.replicationInfoUseCase = replicationInfoUseCase;
        this.failoverSimulationUseCase = failoverSimulationUseCase;
        this.sentinelConfigUseCase = sentinelConfigUseCase;
    }

    /**
     * Retrieves the current replication information from the connected Redis instance.
     *
     * @return the current {@link ReplicationInfo} including role, connected slaves,
     *         and replication offset
     */
    @GetMapping("/replication")
    public ReplicationInfo getReplicationInfo() {
        return replicationInfoUseCase.getReplicationInfo();
    }

    /**
     * Lists all available read-write splitting strategies for master-replica
     * topologies.
     *
     * @return a list of all {@link ReadWriteStrategy} values with descriptions
     */
    @GetMapping("/read-write-strategies")
    public List<ReadWriteStrategy> listStrategies() {
        return replicationInfoUseCase.listReadWriteStrategies();
    }

    /**
     * Simulates the Sentinel failover process and returns an ordered sequence
     * of failover events describing each step from SDOWN detection through
     * master promotion.
     *
     * @return a list of {@link FailoverEvent} in chronological order
     */
    @GetMapping("/failover/sequence")
    public List<FailoverEvent> getFailoverSequence() {
        return failoverSimulationUseCase.simulateFailoverSequence();
    }

    /**
     * Verifies data integrity after a simulated failover by checking how
     * many keys with the given prefix survived the transition.
     *
     * @param keyPrefix     the prefix of keys to verify
     * @param expectedCount the expected total number of keys
     * @return the number of keys that were successfully found
     */
    @PostMapping("/failover/verify")
    public int verifyData(@RequestParam String keyPrefix,
                          @RequestParam int expectedCount) {
        return failoverSimulationUseCase.verifyDataAfterFailover(keyPrefix, expectedCount);
    }

    /**
     * Returns the recommended Sentinel configuration for the given master name.
     *
     * @param masterName the logical name of the master to monitor
     * @return a {@link SentinelConfig} with recommended settings
     */
    @GetMapping("/sentinel/config/{masterName}")
    public SentinelConfig getSentinelConfig(@PathVariable String masterName) {
        return sentinelConfigUseCase.getRecommendedConfig(masterName);
    }
}
