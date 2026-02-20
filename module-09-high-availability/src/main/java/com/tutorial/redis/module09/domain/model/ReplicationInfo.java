package com.tutorial.redis.module09.domain.model;

import java.util.Objects;

/**
 * Represents Redis replication status from the INFO replication command.
 *
 * <p>Captures the key fields reported by {@code INFO replication}:
 * <ul>
 *   <li>{@code role} — "master" or "slave"</li>
 *   <li>{@code connectedSlaves} — number of connected replicas</li>
 *   <li>{@code replicationOffset} — the server's current replication offset</li>
 *   <li>{@code replicationBacklogSize} — size of the replication backlog in bytes</li>
 *   <li>{@code replicationBacklogActive} — whether the backlog is actively in use</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class ReplicationInfo {

    private String role;
    private int connectedSlaves;
    private long replicationOffset;
    private int replicationBacklogSize;
    private boolean replicationBacklogActive;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public ReplicationInfo() {
    }

    /**
     * Creates a ReplicationInfo with the specified values.
     *
     * @param role                     the role of this Redis instance ("master" or "slave")
     * @param connectedSlaves          number of connected replicas
     * @param replicationOffset        the server's current replication offset
     * @param replicationBacklogSize   size of the replication backlog in bytes
     * @param replicationBacklogActive whether the backlog is actively in use
     */
    public ReplicationInfo(String role, int connectedSlaves, long replicationOffset,
                           int replicationBacklogSize, boolean replicationBacklogActive) {
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.connectedSlaves = connectedSlaves;
        this.replicationOffset = replicationOffset;
        this.replicationBacklogSize = replicationBacklogSize;
        this.replicationBacklogActive = replicationBacklogActive;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getConnectedSlaves() {
        return connectedSlaves;
    }

    public void setConnectedSlaves(int connectedSlaves) {
        this.connectedSlaves = connectedSlaves;
    }

    public long getReplicationOffset() {
        return replicationOffset;
    }

    public void setReplicationOffset(long replicationOffset) {
        this.replicationOffset = replicationOffset;
    }

    public int getReplicationBacklogSize() {
        return replicationBacklogSize;
    }

    public void setReplicationBacklogSize(int replicationBacklogSize) {
        this.replicationBacklogSize = replicationBacklogSize;
    }

    public boolean isReplicationBacklogActive() {
        return replicationBacklogActive;
    }

    public void setReplicationBacklogActive(boolean replicationBacklogActive) {
        this.replicationBacklogActive = replicationBacklogActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplicationInfo that)) return false;
        return connectedSlaves == that.connectedSlaves
                && replicationOffset == that.replicationOffset
                && replicationBacklogSize == that.replicationBacklogSize
                && replicationBacklogActive == that.replicationBacklogActive
                && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, connectedSlaves, replicationOffset,
                replicationBacklogSize, replicationBacklogActive);
    }

    @Override
    public String toString() {
        return "ReplicationInfo{role='%s', connectedSlaves=%d, replicationOffset=%d, replicationBacklogSize=%d, replicationBacklogActive=%s}".formatted(
                role, connectedSlaves, replicationOffset, replicationBacklogSize, replicationBacklogActive);
    }
}
