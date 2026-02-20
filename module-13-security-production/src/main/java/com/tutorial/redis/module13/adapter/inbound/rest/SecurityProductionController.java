package com.tutorial.redis.module13.adapter.inbound.rest;

import com.tutorial.redis.module13.domain.model.AclUser;
import com.tutorial.redis.module13.domain.model.EvictionPolicy;
import com.tutorial.redis.module13.domain.model.MemoryInfo;
import com.tutorial.redis.module13.domain.model.ProductionCheckItem;
import com.tutorial.redis.module13.domain.model.ServerMetrics;
import com.tutorial.redis.module13.domain.model.SlowLogEntry;
import com.tutorial.redis.module13.domain.port.inbound.MonitoringUseCase;
import com.tutorial.redis.module13.domain.port.inbound.ProductionChecklistUseCase;
import com.tutorial.redis.module13.domain.port.inbound.SecurityInfoUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for Redis security, monitoring, and production-readiness endpoints.
 *
 * <p>Provides a read-only API for inspecting Redis server configuration and health:</p>
 * <ul>
 *   <li>GET /api/production/acl/users -- list all ACL users</li>
 *   <li>GET /api/production/eviction-policies -- list available eviction policies</li>
 *   <li>GET /api/production/memory -- current memory usage snapshot</li>
 *   <li>GET /api/production/slowlog -- recent slow log entries</li>
 *   <li>GET /api/production/metrics -- aggregated server metrics</li>
 *   <li>GET /api/production/checklist -- production-readiness checklist</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/production")
public class SecurityProductionController {

    private final SecurityInfoUseCase securityInfoUseCase;
    private final MonitoringUseCase monitoringUseCase;
    private final ProductionChecklistUseCase productionChecklistUseCase;

    public SecurityProductionController(SecurityInfoUseCase securityInfoUseCase,
                                        MonitoringUseCase monitoringUseCase,
                                        ProductionChecklistUseCase productionChecklistUseCase) {
        this.securityInfoUseCase = securityInfoUseCase;
        this.monitoringUseCase = monitoringUseCase;
        this.productionChecklistUseCase = productionChecklistUseCase;
    }

    /**
     * Lists all configured Redis ACL users.
     *
     * @return list of ACL users with their permissions
     */
    @GetMapping("/acl/users")
    public List<AclUser> listAclUsers() {
        return securityInfoUseCase.listAclUsers();
    }

    /**
     * Lists all available Redis eviction policies.
     *
     * @return all eviction policy enum values
     */
    @GetMapping("/eviction-policies")
    public List<EvictionPolicy> listEvictionPolicies() {
        return securityInfoUseCase.listEvictionPolicies();
    }

    /**
     * Returns current Redis memory usage information.
     *
     * @return memory info snapshot
     */
    @GetMapping("/memory")
    public MemoryInfo getMemoryInfo() {
        return monitoringUseCase.getMemoryInfo();
    }

    /**
     * Returns the most recent slow log entries.
     *
     * @param count maximum number of entries to return (default 10)
     * @return list of slow log entries
     */
    @GetMapping("/slowlog")
    public List<SlowLogEntry> getSlowLog(@RequestParam(defaultValue = "10") int count) {
        return monitoringUseCase.getSlowLog(count);
    }

    /**
     * Returns aggregated server performance metrics.
     *
     * @return server metrics snapshot
     */
    @GetMapping("/metrics")
    public ServerMetrics getServerMetrics() {
        return monitoringUseCase.getServerMetrics();
    }

    /**
     * Returns the Redis production-readiness checklist.
     *
     * @return list of check items grouped by category
     */
    @GetMapping("/checklist")
    public List<ProductionCheckItem> getChecklist() {
        return productionChecklistUseCase.getChecklist();
    }
}
