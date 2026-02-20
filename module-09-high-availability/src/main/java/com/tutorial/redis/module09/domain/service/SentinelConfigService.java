package com.tutorial.redis.module09.domain.service;

import com.tutorial.redis.module09.domain.model.SentinelConfig;

import java.util.Objects;

/**
 * Pure domain service that generates recommended Sentinel configuration.
 *
 * <p>This service has zero framework dependencies — it operates entirely
 * on domain knowledge about Redis Sentinel best practices. The recommended
 * configuration follows the community-accepted defaults for a typical
 * three-Sentinel, one-master, two-replica deployment:</p>
 * <ul>
 *   <li><strong>quorum = 2</strong> — a majority of 3 Sentinels must agree</li>
 *   <li><strong>downAfterMilliseconds = 5000</strong> — 5 seconds of unresponsiveness
 *       before marking SDOWN</li>
 *   <li><strong>failoverTimeout = 60000</strong> — 60 seconds maximum for the
 *       entire failover operation</li>
 *   <li><strong>parallelSyncs = 1</strong> — one replica re-syncs at a time to
 *       avoid overloading the new master</li>
 * </ul>
 */
public class SentinelConfigService {

    /**
     * Returns the recommended Sentinel configuration for the given master name.
     *
     * @param masterName the logical name of the master to monitor
     * @return a {@link SentinelConfig} with recommended settings
     * @throws NullPointerException if masterName is null
     */
    public SentinelConfig getRecommendedConfig(String masterName) {
        Objects.requireNonNull(masterName, "masterName must not be null");
        return new SentinelConfig(
                masterName,
                2,
                5000,
                60000,
                1
        );
    }
}
