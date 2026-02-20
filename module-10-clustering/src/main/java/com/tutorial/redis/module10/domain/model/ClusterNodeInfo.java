package com.tutorial.redis.module10.domain.model;

import java.util.Objects;

/**
 * Represents information about a single node in a Redis Cluster.
 *
 * <p>Each node in a Redis Cluster is either a master (responsible for a range
 * of hash slots) or a replica (replicating a specific master). This model
 * captures:
 * <ul>
 *   <li>{@code nodeId} — unique identifier for this node</li>
 *   <li>{@code address} — the ip:port address of the node</li>
 *   <li>{@code role} — "master" or "slave"</li>
 *   <li>{@code slotStart} — start of the hash slot range assigned to this node</li>
 *   <li>{@code slotEnd} — end of the hash slot range assigned to this node</li>
 *   <li>{@code masterId} — null for masters, the master's nodeId for replicas</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class ClusterNodeInfo {

    private String nodeId;
    private String address;
    private String role;
    private int slotStart;
    private int slotEnd;
    private String masterId;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public ClusterNodeInfo() {
    }

    /**
     * Creates a ClusterNodeInfo with the specified values.
     *
     * @param nodeId    unique identifier for this node
     * @param address   the ip:port address of the node
     * @param role      "master" or "slave"
     * @param slotStart start of the hash slot range
     * @param slotEnd   end of the hash slot range
     * @param masterId  null for masters, master's nodeId for replicas
     */
    public ClusterNodeInfo(String nodeId, String address, String role,
                           int slotStart, int slotEnd, String masterId) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
        this.address = Objects.requireNonNull(address, "address must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
        this.masterId = masterId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getSlotStart() {
        return slotStart;
    }

    public void setSlotStart(int slotStart) {
        this.slotStart = slotStart;
    }

    public int getSlotEnd() {
        return slotEnd;
    }

    public void setSlotEnd(int slotEnd) {
        this.slotEnd = slotEnd;
    }

    public String getMasterId() {
        return masterId;
    }

    public void setMasterId(String masterId) {
        this.masterId = masterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClusterNodeInfo that)) return false;
        return slotStart == that.slotStart
                && slotEnd == that.slotEnd
                && Objects.equals(nodeId, that.nodeId)
                && Objects.equals(address, that.address)
                && Objects.equals(role, that.role)
                && Objects.equals(masterId, that.masterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, address, role, slotStart, slotEnd, masterId);
    }

    @Override
    public String toString() {
        return "ClusterNodeInfo{nodeId='%s', address='%s', role='%s', slotStart=%d, slotEnd=%d, masterId='%s'}".formatted(
                nodeId, address, role, slotStart, slotEnd, masterId);
    }
}
