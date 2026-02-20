package com.tutorial.redis.module10.domain.service;

import com.tutorial.redis.module10.domain.model.ClusterNodeInfo;
import com.tutorial.redis.module10.domain.model.ClusterTopology;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pure domain service that generates recommended Redis Cluster topologies.
 *
 * <p>This service has zero framework dependencies and produces educational
 * cluster topology configurations showing how 16384 hash slots are distributed
 * across master nodes, each backed by a replica for high availability.</p>
 *
 * <p>The generated topologies follow Redis Cluster best practices:
 * <ul>
 *   <li>Slots are distributed as evenly as possible across masters</li>
 *   <li>Each master has exactly one replica</li>
 *   <li>The recommended topology is 3 masters + 3 replicas (6 nodes total)</li>
 * </ul>
 */
public class ClusterTopologyService {

    /** Total number of hash slots in a Redis Cluster. */
    private static final int TOTAL_SLOTS = 16384;

    /** Base port number for generating node addresses. */
    private static final int BASE_PORT = 7000;

    /**
     * Returns the recommended 6-node cluster topology.
     *
     * <p>The standard Redis Cluster recommendation is 3 master nodes with
     * 1 replica each, distributing the 16384 hash slots as follows:
     * <ul>
     *   <li>Master 1: slots 0-5460</li>
     *   <li>Master 2: slots 5461-10922</li>
     *   <li>Master 3: slots 10923-16383</li>
     * </ul>
     *
     * @return a {@link ClusterTopology} with 3 masters and 3 replicas
     */
    public ClusterTopology generateRecommendedTopology() {
        return generateTopology(3);
    }

    /**
     * Generates a cluster topology with the given number of masters,
     * each backed by one replica, distributing 16384 slots evenly.
     *
     * @param masterCount the number of master nodes (must be at least 1)
     * @return a {@link ClusterTopology} with the specified number of masters and replicas
     * @throws IllegalArgumentException if masterCount is less than 1
     */
    public ClusterTopology generateTopology(int masterCount) {
        if (masterCount < 1) {
            throw new IllegalArgumentException("masterCount must be at least 1");
        }

        List<ClusterNodeInfo> nodes = new ArrayList<>();
        int slotsPerMaster = TOTAL_SLOTS / masterCount;
        int remainingSlots = TOTAL_SLOTS % masterCount;

        int currentSlotStart = 0;

        for (int i = 0; i < masterCount; i++) {
            String masterId = generateNodeId();
            int slotCount = slotsPerMaster + (i < remainingSlots ? 1 : 0);
            int slotEnd = currentSlotStart + slotCount - 1;

            // Create master node
            ClusterNodeInfo master = new ClusterNodeInfo(
                    masterId,
                    "127.0.0.1:" + (BASE_PORT + i),
                    "master",
                    currentSlotStart,
                    slotEnd,
                    null
            );
            nodes.add(master);

            // Create replica node for this master
            ClusterNodeInfo replica = new ClusterNodeInfo(
                    generateNodeId(),
                    "127.0.0.1:" + (BASE_PORT + masterCount + i),
                    "slave",
                    currentSlotStart,
                    slotEnd,
                    masterId
            );
            nodes.add(replica);

            currentSlotStart = slotEnd + 1;
        }

        int totalNodes = masterCount * 2;
        return new ClusterTopology(totalNodes, masterCount, masterCount, TOTAL_SLOTS, nodes);
    }

    /**
     * Generates a unique node identifier.
     *
     * <p>In a real Redis Cluster, node IDs are 40-character hex strings.
     * This implementation uses a UUID-based approach for simplicity.</p>
     *
     * @return a unique node identifier string
     */
    private String generateNodeId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
