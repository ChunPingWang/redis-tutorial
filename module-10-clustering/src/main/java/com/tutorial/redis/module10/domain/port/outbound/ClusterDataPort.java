package com.tutorial.redis.module10.domain.port.outbound;

import java.util.List;
import java.util.Map;

/**
 * Outbound port for reading and writing data in a Redis Cluster environment.
 *
 * <p>This port abstracts the data access operations that a cluster-aware
 * application needs. Implementations are responsible for handling the
 * underlying Redis communication, including any cluster-specific concerns
 * such as slot redirection.</p>
 *
 * <p>In a real cluster deployment, multi-key operations (e.g.
 * {@link #writeMultipleKeys(Map)} and {@link #readMultipleKeys(List)})
 * require all keys to reside in the same hash slot. Callers should use
 * hash tags to ensure co-location when performing multi-key operations.</p>
 */
public interface ClusterDataPort {

    /**
     * Writes a single key-value pair.
     *
     * @param key   the key to write
     * @param value the value to store
     */
    void writeData(String key, String value);

    /**
     * Reads the value associated with the given key.
     *
     * @param key the key to read
     * @return the value, or null if the key does not exist
     */
    String readData(String key);

    /**
     * Writes multiple key-value pairs. In a cluster environment, all keys
     * should share the same hash tag to ensure they map to the same slot.
     *
     * @param keyValues a map of key-value pairs to write
     */
    void writeMultipleKeys(Map<String, String> keyValues);

    /**
     * Reads multiple keys at once and returns a map of key-value pairs.
     * Keys that do not exist will have null values in the returned map.
     *
     * @param keys the list of keys to read
     * @return a map of key-value pairs (values may be null for missing keys)
     */
    Map<String, String> readMultipleKeys(List<String> keys);
}
