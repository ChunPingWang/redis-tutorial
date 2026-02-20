package com.tutorial.redis.module13.domain.service;

import com.tutorial.redis.module13.domain.model.ProductionCheckItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain service that generates a Redis production-readiness checklist.
 *
 * <p>This service encapsulates the domain knowledge about what constitutes
 * a production-ready Redis deployment. Each item represents a best practice
 * or configuration setting that should be reviewed before going live.</p>
 *
 * <p>Categories covered:</p>
 * <ul>
 *   <li><b>Security</b> -- authentication, ACL, network binding, TLS</li>
 *   <li><b>Memory</b> -- maxmemory, eviction policy, monitoring</li>
 *   <li><b>Persistence</b> -- RDB/AOF configuration, backup strategy</li>
 *   <li><b>High Availability</b> -- replication, Sentinel/Cluster setup</li>
 *   <li><b>Monitoring</b> -- metrics, slow log, alerting</li>
 *   <li><b>Performance</b> -- connection pooling, pipelining, key design</li>
 * </ul>
 *
 * <p>Items are returned with {@code checked = false} by default because
 * the checklist is meant to be reviewed manually by operations teams.</p>
 */
public class ProductionChecklistService {

    /**
     * Generates the full production-readiness checklist.
     *
     * @return an ordered list of checklist items grouped by category
     */
    public List<ProductionCheckItem> getChecklist() {
        List<ProductionCheckItem> checklist = new ArrayList<>();

        // Security
        checklist.add(new ProductionCheckItem("Security",
                "Set a strong requirepass or use ACL users with passwords", false));
        checklist.add(new ProductionCheckItem("Security",
                "Disable or rename dangerous commands (FLUSHALL, FLUSHDB, DEBUG, KEYS)", false));
        checklist.add(new ProductionCheckItem("Security",
                "Bind Redis to specific network interfaces (not 0.0.0.0)", false));
        checklist.add(new ProductionCheckItem("Security",
                "Enable TLS for client-server and replication connections", false));
        checklist.add(new ProductionCheckItem("Security",
                "Configure ACL users with least-privilege command and key permissions", false));
        checklist.add(new ProductionCheckItem("Security",
                "Enable protected-mode when no authentication is configured", false));

        // Memory
        checklist.add(new ProductionCheckItem("Memory",
                "Set maxmemory to a value below the available system memory", false));
        checklist.add(new ProductionCheckItem("Memory",
                "Choose an appropriate eviction policy (e.g. allkeys-lru for caches)", false));
        checklist.add(new ProductionCheckItem("Memory",
                "Monitor memory usage and set alerts for high utilization", false));
        checklist.add(new ProductionCheckItem("Memory",
                "Disable Transparent Huge Pages (THP) on the host OS", false));

        // Persistence
        checklist.add(new ProductionCheckItem("Persistence",
                "Configure RDB snapshots or AOF (or both) based on durability needs", false));
        checklist.add(new ProductionCheckItem("Persistence",
                "Set up regular off-host backups of RDB/AOF files", false));
        checklist.add(new ProductionCheckItem("Persistence",
                "Test restore procedures from backups periodically", false));
        checklist.add(new ProductionCheckItem("Persistence",
                "Use appendfsync everysec for AOF as a balance of safety and performance", false));

        // High Availability
        checklist.add(new ProductionCheckItem("High Availability",
                "Deploy Redis replicas for read scaling and failover", false));
        checklist.add(new ProductionCheckItem("High Availability",
                "Use Redis Sentinel or Cluster for automatic failover", false));
        checklist.add(new ProductionCheckItem("High Availability",
                "Configure appropriate replication backlog size", false));
        checklist.add(new ProductionCheckItem("High Availability",
                "Test failover scenarios regularly", false));

        // Monitoring
        checklist.add(new ProductionCheckItem("Monitoring",
                "Export Redis metrics to a monitoring system (Prometheus, Datadog, etc.)", false));
        checklist.add(new ProductionCheckItem("Monitoring",
                "Configure slowlog-log-slower-than and review slow log regularly", false));
        checklist.add(new ProductionCheckItem("Monitoring",
                "Set up alerts for connected_clients, memory usage, and replication lag", false));
        checklist.add(new ProductionCheckItem("Monitoring",
                "Monitor keyspace hit rate to detect cache effectiveness issues", false));

        // Performance
        checklist.add(new ProductionCheckItem("Performance",
                "Use connection pooling in application clients", false));
        checklist.add(new ProductionCheckItem("Performance",
                "Batch operations with pipelines where possible", false));
        checklist.add(new ProductionCheckItem("Performance",
                "Avoid large keys and use appropriate data structures", false));
        checklist.add(new ProductionCheckItem("Performance",
                "Set appropriate timeout values for client connections", false));

        return checklist;
    }
}
