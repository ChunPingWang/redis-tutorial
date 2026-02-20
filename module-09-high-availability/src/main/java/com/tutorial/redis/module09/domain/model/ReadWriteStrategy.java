package com.tutorial.redis.module09.domain.model;

/**
 * Enum representing read-write splitting strategies for Redis replication topologies.
 *
 * <p>In a Redis master-replica setup, reads can be distributed across replicas
 * to reduce load on the master while writes always go to the master. Each
 * strategy offers a different trade-off between consistency and throughput:</p>
 * <ul>
 *   <li>{@link #MASTER_ONLY} — safest; no stale reads, but master handles all load</li>
 *   <li>{@link #REPLICA_PREFERRED} — reads prefer replicas but fall back to master</li>
 *   <li>{@link #REPLICA} — reads only go to replicas; may observe replication lag</li>
 *   <li>{@link #ANY_REPLICA} — reads go to any available replica for load balancing</li>
 * </ul>
 */
public enum ReadWriteStrategy {

    MASTER_ONLY("所有讀寫都在 Master"),
    REPLICA_PREFERRED("讀取優先走 Replica，寫入走 Master"),
    REPLICA("讀取只走 Replica，寫入走 Master"),
    ANY_REPLICA("讀取走任意 Replica，寫入走 Master");

    private final String description;

    ReadWriteStrategy(String description) {
        this.description = description;
    }

    /**
     * Returns the human-readable description of this strategy.
     *
     * @return description in Traditional Chinese
     */
    public String getDescription() {
        return description;
    }
}
