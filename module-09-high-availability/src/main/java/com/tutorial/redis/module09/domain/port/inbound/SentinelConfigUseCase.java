package com.tutorial.redis.module09.domain.port.inbound;

import com.tutorial.redis.module09.domain.model.FailoverEvent;
import com.tutorial.redis.module09.domain.model.SentinelConfig;

import java.util.List;

/**
 * Inbound port: retrieving Sentinel configuration recommendations
 * and describing the failover process.
 *
 * <p>Provides the application-level API for obtaining best-practice
 * Sentinel configuration and a step-by-step description of how
 * Sentinel performs automatic failover.</p>
 */
public interface SentinelConfigUseCase {

    /**
     * Returns the recommended Sentinel configuration for the given master name.
     *
     * @param masterName the logical name of the master to monitor
     * @return a {@link SentinelConfig} with recommended settings
     */
    SentinelConfig getRecommendedConfig(String masterName);

    /**
     * Describes the Sentinel failover process as a sequence of events.
     *
     * @return a list of {@link FailoverEvent} describing each step of the failover
     */
    List<FailoverEvent> describeFailoverProcess();
}
