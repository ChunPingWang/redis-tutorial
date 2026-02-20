package com.tutorial.redis.module09.domain.model;

import java.util.Objects;

/**
 * Represents Sentinel configuration knowledge (informational, not runtime).
 *
 * <p>Encapsulates the recommended settings for a Redis Sentinel deployment:
 * <ul>
 *   <li>{@code masterName} — the logical name of the master monitored by Sentinels</li>
 *   <li>{@code quorum} — number of Sentinels that must agree to trigger failover</li>
 *   <li>{@code downAfterMilliseconds} — time after which a non-responsive master is
 *       considered subjectively down (SDOWN)</li>
 *   <li>{@code failoverTimeout} — maximum time allowed for a failover operation</li>
 *   <li>{@code parallelSyncs} — number of replicas that can re-sync with the new master
 *       simultaneously after a failover</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class SentinelConfig {

    private String masterName;
    private int quorum;
    private int downAfterMilliseconds;
    private int failoverTimeout;
    private int parallelSyncs;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public SentinelConfig() {
    }

    /**
     * Creates a SentinelConfig with the specified values.
     *
     * @param masterName            the logical name of the monitored master
     * @param quorum                number of Sentinels needed to agree for failover
     * @param downAfterMilliseconds milliseconds before marking master as SDOWN
     * @param failoverTimeout       maximum failover duration in milliseconds
     * @param parallelSyncs         number of replicas to re-sync in parallel
     */
    public SentinelConfig(String masterName, int quorum, int downAfterMilliseconds,
                          int failoverTimeout, int parallelSyncs) {
        this.masterName = Objects.requireNonNull(masterName, "masterName must not be null");
        this.quorum = quorum;
        this.downAfterMilliseconds = downAfterMilliseconds;
        this.failoverTimeout = failoverTimeout;
        this.parallelSyncs = parallelSyncs;
    }

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    public int getQuorum() {
        return quorum;
    }

    public void setQuorum(int quorum) {
        this.quorum = quorum;
    }

    public int getDownAfterMilliseconds() {
        return downAfterMilliseconds;
    }

    public void setDownAfterMilliseconds(int downAfterMilliseconds) {
        this.downAfterMilliseconds = downAfterMilliseconds;
    }

    public int getFailoverTimeout() {
        return failoverTimeout;
    }

    public void setFailoverTimeout(int failoverTimeout) {
        this.failoverTimeout = failoverTimeout;
    }

    public int getParallelSyncs() {
        return parallelSyncs;
    }

    public void setParallelSyncs(int parallelSyncs) {
        this.parallelSyncs = parallelSyncs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SentinelConfig that)) return false;
        return quorum == that.quorum
                && downAfterMilliseconds == that.downAfterMilliseconds
                && failoverTimeout == that.failoverTimeout
                && parallelSyncs == that.parallelSyncs
                && Objects.equals(masterName, that.masterName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterName, quorum, downAfterMilliseconds, failoverTimeout, parallelSyncs);
    }

    @Override
    public String toString() {
        return "SentinelConfig{masterName='%s', quorum=%d, downAfterMilliseconds=%d, failoverTimeout=%d, parallelSyncs=%d}".formatted(
                masterName, quorum, downAfterMilliseconds, failoverTimeout, parallelSyncs);
    }
}
