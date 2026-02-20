package com.tutorial.redis.module03.domain.port.inbound;

/**
 * Inbound port: check for duplicates using Redis Bloom filter.
 */
public interface CheckDuplicateUseCase {

    /**
     * Initializes a Bloom filter for duplicate detection.
     *
     * @param filterName the name of the Bloom filter
     * @param errorRate  the desired false positive error rate (e.g., 0.01 for 1%)
     * @param capacity   the expected number of items to be inserted
     */
    void initializeFilter(String filterName, double errorRate, long capacity);

    /**
     * Marks an item as processed, adding it to the Bloom filter.
     *
     * @param filterName the name of the Bloom filter
     * @param itemId     the item identifier to mark as processed
     * @return true if the item was newly added (not previously processed)
     */
    boolean markAsProcessed(String filterName, String itemId);

    /**
     * Checks whether an item might have been processed already.
     *
     * @param filterName the name of the Bloom filter
     * @param itemId     the item identifier to check
     * @return true if the item might have been processed (may be a false positive)
     */
    boolean mightBeProcessed(String filterName, String itemId);
}
