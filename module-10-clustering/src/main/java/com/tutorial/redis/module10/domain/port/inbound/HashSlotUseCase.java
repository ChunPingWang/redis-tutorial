package com.tutorial.redis.module10.domain.port.inbound;

import com.tutorial.redis.module10.domain.model.HashSlotInfo;
import com.tutorial.redis.module10.domain.model.HashTagAnalysis;

import java.util.List;

/**
 * Inbound port for hash slot calculation operations.
 *
 * <p>Provides use cases for computing Redis Cluster hash slots for individual
 * keys and for analyzing whether a group of keys shares the same hash slot
 * via hash tags.</p>
 */
public interface HashSlotUseCase {

    /**
     * Calculates the hash slot for a given key.
     *
     * @param key the Redis key to analyze
     * @return a {@link HashSlotInfo} containing the key, slot, and hash tag (if any)
     */
    HashSlotInfo calculateSlot(String key);

    /**
     * Analyzes a list of keys to determine if they share the same hash tag
     * and are therefore co-located in the same hash slot.
     *
     * @param keys the list of keys to analyze
     * @return a {@link HashTagAnalysis} describing slot co-location
     */
    HashTagAnalysis analyzeHashTag(List<String> keys);
}
