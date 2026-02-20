package com.tutorial.redis.module13.domain.model;

/**
 * Aggregated server performance metrics from Redis INFO.
 *
 * <p>Combines data from multiple INFO sections (server, clients, stats) into
 * a single snapshot that is useful for monitoring dashboards and production
 * health checks.</p>
 *
 * <ul>
 *   <li>{@code connectedClients} -- number of currently connected clients</li>
 *   <li>{@code keyspaceHits} / {@code keyspaceMisses} -- cumulative hit/miss counters</li>
 *   <li>{@code hitRate} -- computed as hits / (hits + misses)</li>
 *   <li>{@code instantaneousOpsPerSec} -- real-time operations per second</li>
 *   <li>{@code totalCommandsProcessed} -- total commands since server start</li>
 *   <li>{@code uptimeInSeconds} -- server uptime</li>
 * </ul>
 */
public class ServerMetrics {

    private final long connectedClients;
    private final long keyspaceHits;
    private final long keyspaceMisses;
    private final double hitRate;
    private final long instantaneousOpsPerSec;
    private final long totalCommandsProcessed;
    private final long uptimeInSeconds;

    public ServerMetrics(long connectedClients, long keyspaceHits, long keyspaceMisses,
                         double hitRate, long instantaneousOpsPerSec,
                         long totalCommandsProcessed, long uptimeInSeconds) {
        this.connectedClients = connectedClients;
        this.keyspaceHits = keyspaceHits;
        this.keyspaceMisses = keyspaceMisses;
        this.hitRate = hitRate;
        this.instantaneousOpsPerSec = instantaneousOpsPerSec;
        this.totalCommandsProcessed = totalCommandsProcessed;
        this.uptimeInSeconds = uptimeInSeconds;
    }

    public long getConnectedClients() {
        return connectedClients;
    }

    public long getKeyspaceHits() {
        return keyspaceHits;
    }

    public long getKeyspaceMisses() {
        return keyspaceMisses;
    }

    public double getHitRate() {
        return hitRate;
    }

    public long getInstantaneousOpsPerSec() {
        return instantaneousOpsPerSec;
    }

    public long getTotalCommandsProcessed() {
        return totalCommandsProcessed;
    }

    public long getUptimeInSeconds() {
        return uptimeInSeconds;
    }

    @Override
    public String toString() {
        return "ServerMetrics{connectedClients=" + connectedClients
                + ", keyspaceHits=" + keyspaceHits
                + ", keyspaceMisses=" + keyspaceMisses
                + ", hitRate=" + String.format("%.4f", hitRate)
                + ", instantaneousOpsPerSec=" + instantaneousOpsPerSec
                + ", totalCommandsProcessed=" + totalCommandsProcessed
                + ", uptimeInSeconds=" + uptimeInSeconds + '}';
    }
}
