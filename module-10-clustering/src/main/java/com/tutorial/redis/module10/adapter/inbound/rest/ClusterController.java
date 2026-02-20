package com.tutorial.redis.module10.adapter.inbound.rest;

import com.tutorial.redis.module10.domain.model.ClusterTopology;
import com.tutorial.redis.module10.domain.model.HashSlotInfo;
import com.tutorial.redis.module10.domain.model.HashTagAnalysis;
import com.tutorial.redis.module10.domain.port.inbound.ClusterDataUseCase;
import com.tutorial.redis.module10.domain.port.inbound.ClusterTopologyUseCase;
import com.tutorial.redis.module10.domain.port.inbound.HashSlotUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing endpoints for Redis Cluster operations:
 * <ul>
 *   <li>Hash slot calculation for individual keys</li>
 *   <li>Hash tag analysis for groups of keys</li>
 *   <li>Cluster topology generation (recommended and custom)</li>
 *   <li>Basic data read/write operations</li>
 * </ul>
 *
 * <p>All endpoints delegate to inbound use case ports, maintaining
 * the hexagonal architecture boundary between the adapter and
 * application layers.</p>
 */
@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    private final HashSlotUseCase hashSlotUseCase;
    private final ClusterTopologyUseCase clusterTopologyUseCase;
    private final ClusterDataUseCase clusterDataUseCase;

    public ClusterController(HashSlotUseCase hashSlotUseCase,
                             ClusterTopologyUseCase clusterTopologyUseCase,
                             ClusterDataUseCase clusterDataUseCase) {
        this.hashSlotUseCase = hashSlotUseCase;
        this.clusterTopologyUseCase = clusterTopologyUseCase;
        this.clusterDataUseCase = clusterDataUseCase;
    }

    /**
     * Calculates the hash slot for a given key.
     *
     * @param key the Redis key to analyze
     * @return the {@link HashSlotInfo} containing the key, slot, and hash tag (if any)
     */
    @GetMapping("/slot/{key}")
    public HashSlotInfo calculateSlot(@PathVariable String key) {
        return hashSlotUseCase.calculateSlot(key);
    }

    /**
     * Analyzes a list of keys to determine if they share the same hash tag
     * and are co-located in the same hash slot.
     *
     * @param keys the list of keys to analyze
     * @return a {@link HashTagAnalysis} describing slot co-location
     */
    @PostMapping("/hash-tag/analyze")
    public HashTagAnalysis analyzeHashTag(@RequestBody List<String> keys) {
        return hashSlotUseCase.analyzeHashTag(keys);
    }

    /**
     * Returns the recommended cluster topology (3 masters + 3 replicas).
     *
     * @return the recommended {@link ClusterTopology}
     */
    @GetMapping("/topology")
    public ClusterTopology getTopology() {
        return clusterTopologyUseCase.getRecommendedTopology();
    }

    /**
     * Returns a cluster topology with the specified number of master nodes.
     *
     * @param masterCount the number of master nodes (must be at least 3)
     * @return a {@link ClusterTopology} with the specified configuration
     */
    @GetMapping("/topology/{masterCount}")
    public ClusterTopology getTopology(@PathVariable int masterCount) {
        return clusterTopologyUseCase.getTopology(masterCount);
    }

    /**
     * Writes a single key-value pair to Redis.
     *
     * @param key   the key to write
     * @param value the value to store
     */
    @PostMapping("/data")
    public void writeData(@RequestParam String key, @RequestParam String value) {
        clusterDataUseCase.writeData(key, value);
    }

    /**
     * Reads the value associated with the given key from Redis.
     *
     * @param key the key to read
     * @return the value, or null if the key does not exist
     */
    @GetMapping("/data/{key}")
    public String readData(@PathVariable String key) {
        return clusterDataUseCase.readData(key);
    }
}
