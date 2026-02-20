package com.tutorial.redis.module10.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents the overall topology of a Redis Cluster.
 *
 * <p>A Redis Cluster distributes data across multiple master nodes, each
 * responsible for a subset of the 16384 hash slots. Each master may have
 * one or more replicas for high availability. This model captures:
 * <ul>
 *   <li>{@code totalNodes} — the total number of nodes (masters + replicas)</li>
 *   <li>{@code masterCount} — the number of master nodes</li>
 *   <li>{@code replicaCount} — the number of replica nodes</li>
 *   <li>{@code totalSlots} — the total number of hash slots (always 16384)</li>
 *   <li>{@code nodes} — detailed information about each node</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class ClusterTopology {

    private int totalNodes;
    private int masterCount;
    private int replicaCount;
    private int totalSlots;
    private List<ClusterNodeInfo> nodes;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public ClusterTopology() {
    }

    /**
     * Creates a ClusterTopology with the specified values.
     *
     * @param totalNodes   the total number of nodes (masters + replicas)
     * @param masterCount  the number of master nodes
     * @param replicaCount the number of replica nodes
     * @param totalSlots   the total number of hash slots (16384)
     * @param nodes        detailed information about each node
     */
    public ClusterTopology(int totalNodes, int masterCount, int replicaCount,
                           int totalSlots, List<ClusterNodeInfo> nodes) {
        this.totalNodes = totalNodes;
        this.masterCount = masterCount;
        this.replicaCount = replicaCount;
        this.totalSlots = totalSlots;
        this.nodes = Objects.requireNonNull(nodes, "nodes must not be null");
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }

    public int getMasterCount() {
        return masterCount;
    }

    public void setMasterCount(int masterCount) {
        this.masterCount = masterCount;
    }

    public int getReplicaCount() {
        return replicaCount;
    }

    public void setReplicaCount(int replicaCount) {
        this.replicaCount = replicaCount;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }

    public List<ClusterNodeInfo> getNodes() {
        return nodes;
    }

    public void setNodes(List<ClusterNodeInfo> nodes) {
        this.nodes = nodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClusterTopology that)) return false;
        return totalNodes == that.totalNodes
                && masterCount == that.masterCount
                && replicaCount == that.replicaCount
                && totalSlots == that.totalSlots
                && Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalNodes, masterCount, replicaCount, totalSlots, nodes);
    }

    @Override
    public String toString() {
        return "ClusterTopology{totalNodes=%d, masterCount=%d, replicaCount=%d, totalSlots=%d, nodes=%s}".formatted(
                totalNodes, masterCount, replicaCount, totalSlots, nodes);
    }
}
