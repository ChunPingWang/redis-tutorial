package com.tutorial.redis.module03.domain.port.outbound;

import java.util.List;

/**
 * Outbound port for Bloom filter operations.
 * Uses Redis Bloom filter commands (BF.RESERVE / BF.ADD / BF.EXISTS / BF.MADD / BF.MEXISTS).
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface BloomFilterPort {

    /**
     * Creates a new Bloom filter with the specified error rate and capacity (BF.RESERVE).
     *
     * @param filterName the name of the Bloom filter
     * @param errorRate  the desired false positive error rate (e.g., 0.01 for 1%)
     * @param capacity   the expected number of items to be inserted
     */
    void createFilter(String filterName, double errorRate, long capacity);

    /**
     * Adds a single item to the Bloom filter (BF.ADD).
     *
     * @param filterName the name of the Bloom filter
     * @param item       the item to add
     * @return true if the item was newly added (not previously present)
     */
    boolean add(String filterName, String item);

    /**
     * Checks whether an item might exist in the Bloom filter (BF.EXISTS).
     *
     * @param filterName the name of the Bloom filter
     * @param item       the item to check
     * @return true if the item might be in the filter (may be a false positive)
     */
    boolean mightContain(String filterName, String item);

    /**
     * Adds multiple items to the Bloom filter in bulk (BF.MADD).
     *
     * @param filterName the name of the Bloom filter
     * @param items      the items to add
     * @return a list of booleans indicating whether each item was newly added
     */
    List<Boolean> addAll(String filterName, List<String> items);

    /**
     * Checks whether multiple items might exist in the Bloom filter (BF.MEXISTS).
     *
     * @param filterName the name of the Bloom filter
     * @param items      the items to check
     * @return a list of booleans indicating whether each item might be present
     */
    List<Boolean> mightContainAll(String filterName, List<String> items);
}
