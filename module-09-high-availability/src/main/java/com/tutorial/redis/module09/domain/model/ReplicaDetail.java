package com.tutorial.redis.module09.domain.model;

import java.util.Objects;

/**
 * Details about a connected replica in a Redis replication topology.
 *
 * <p>Each field corresponds to information available from the master's
 * {@code INFO replication} output for each connected slave:
 * <ul>
 *   <li>{@code id} — unique identifier of the replica</li>
 *   <li>{@code ip} — IP address of the replica</li>
 *   <li>{@code port} — port number of the replica</li>
 *   <li>{@code state} — connection state ("online", "wait_bgsave", etc.)</li>
 *   <li>{@code offset} — the replica's current replication offset</li>
 *   <li>{@code lag} — replication lag in seconds</li>
 * </ul>
 *
 * <p>Non-final fields with getters/setters to support Jackson NON_FINAL default typing.</p>
 */
public class ReplicaDetail {

    private String id;
    private String ip;
    private int port;
    private String state;
    private long offset;
    private long lag;

    /**
     * No-arg constructor for deserialization frameworks.
     */
    public ReplicaDetail() {
    }

    /**
     * Creates a ReplicaDetail with the specified values.
     *
     * @param id     unique identifier of the replica
     * @param ip     IP address of the replica
     * @param port   port number of the replica
     * @param state  connection state ("online", "wait_bgsave", etc.)
     * @param offset the replica's current replication offset
     * @param lag    replication lag in seconds
     */
    public ReplicaDetail(String id, String ip, int port, String state, long offset, long lag) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ip = Objects.requireNonNull(ip, "ip must not be null");
        this.port = port;
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.offset = offset;
        this.lag = lag;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getLag() {
        return lag;
    }

    public void setLag(long lag) {
        this.lag = lag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplicaDetail that)) return false;
        return port == that.port
                && offset == that.offset
                && lag == that.lag
                && Objects.equals(id, that.id)
                && Objects.equals(ip, that.ip)
                && Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ip, port, state, offset, lag);
    }

    @Override
    public String toString() {
        return "ReplicaDetail{id='%s', ip='%s', port=%d, state='%s', offset=%d, lag=%d}".formatted(
                id, ip, port, state, offset, lag);
    }
}
