package com.tutorial.redis.module10.domain.port.inbound;

import java.util.Map;

/**
 * Inbound port for cluster data operations.
 *
 * <p>Provides use cases for reading and writing data in a cluster-aware
 * manner, including hash-tag-based operations that ensure multiple keys
 * are co-located in the same hash slot.</p>
 */
public interface ClusterDataUseCase {

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
     * Writes multiple key-value pairs using a hash tag to ensure all keys
     * map to the same hash slot. Keys are formatted as {@code {hashTag}:subKey}.
     *
     * @param hashTag      the hash tag to use for slot co-location
     * @param subKeyValues a map of sub-key to value pairs
     */
    void writeWithHashTag(String hashTag, Map<String, String> subKeyValues);

    /**
     * Reads multiple keys using a hash tag. Keys are formatted as
     * {@code {hashTag}:subKey}.
     *
     * @param hashTag the hash tag used for slot co-location
     * @param subKeys the list of sub-keys to read
     * @return a map of sub-key to value pairs (values may be null for missing keys)
     */
    Map<String, String> readWithHashTag(String hashTag, java.util.List<String> subKeys);
}
