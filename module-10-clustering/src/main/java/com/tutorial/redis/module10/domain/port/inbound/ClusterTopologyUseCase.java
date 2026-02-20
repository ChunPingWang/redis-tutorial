package com.tutorial.redis.module10.domain.port.inbound;

import com.tutorial.redis.module10.domain.model.ClusterTopology;

/**
 * Inbound port for cluster topology operations.
 *
 * <p>Provides use cases for generating and querying Redis Cluster topology
 * configurations, including the recommended default topology and custom
 * topologies with a specified number of master nodes.</p>
 */
public interface ClusterTopologyUseCase {

    /**
     * Returns the recommended cluster topology (3 masters + 3 replicas).
     *
     * @return the recommended {@link ClusterTopology}
     */
    ClusterTopology getRecommendedTopology();

    /**
     * Returns a cluster topology with the specified number of master nodes.
     * Each master is paired with one replica.
     *
     * @param masterCount the number of master nodes (must be at least 3)
     * @return a {@link ClusterTopology} with the specified configuration
     */
    ClusterTopology getTopology(int masterCount);
}
