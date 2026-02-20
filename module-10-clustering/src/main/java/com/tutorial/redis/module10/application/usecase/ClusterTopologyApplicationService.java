package com.tutorial.redis.module10.application.usecase;

import com.tutorial.redis.module10.domain.model.ClusterTopology;
import com.tutorial.redis.module10.domain.port.inbound.ClusterTopologyUseCase;
import com.tutorial.redis.module10.domain.service.ClusterTopologyService;
import org.springframework.stereotype.Service;

/**
 * Application service implementing {@link ClusterTopologyUseCase}.
 *
 * <p>Delegates topology generation to the {@link ClusterTopologyService}
 * domain service. This thin application layer exists to decouple the
 * inbound adapter (REST controller) from the domain service, following
 * hexagonal architecture conventions.</p>
 */
@Service
public class ClusterTopologyApplicationService implements ClusterTopologyUseCase {

    private final ClusterTopologyService clusterTopologyService;

    public ClusterTopologyApplicationService(ClusterTopologyService clusterTopologyService) {
        this.clusterTopologyService = clusterTopologyService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClusterTopology getRecommendedTopology() {
        return clusterTopologyService.generateRecommendedTopology();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClusterTopology getTopology(int masterCount) {
        return clusterTopologyService.generateTopology(masterCount);
    }
}
