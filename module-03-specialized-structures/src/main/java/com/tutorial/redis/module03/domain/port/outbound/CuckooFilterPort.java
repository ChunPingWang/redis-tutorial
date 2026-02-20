package com.tutorial.redis.module03.domain.port.outbound;

/**
 * Outbound port for Cuckoo filter operations.
 * Uses Redis Cuckoo filter commands (CF.RESERVE / CF.ADD / CF.EXISTS / CF.DEL).
 * Cuckoo filters support deletion unlike Bloom filters.
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface CuckooFilterPort {

    /**
     * Creates a new Cuckoo filter with the specified capacity (CF.RESERVE).
     *
     * @param filterName the name of the Cuckoo filter
     * @param capacity   the expected number of items to be inserted
     */
    void createFilter(String filterName, long capacity);

    /**
     * Adds an item to the Cuckoo filter (CF.ADD).
     *
     * @param filterName the name of the Cuckoo filter
     * @param item       the item to add
     * @return true if the item was successfully added
     */
    boolean add(String filterName, String item);

    /**
     * Checks whether an item might exist in the Cuckoo filter (CF.EXISTS).
     *
     * @param filterName the name of the Cuckoo filter
     * @param item       the item to check
     * @return true if the item might be in the filter
     */
    boolean mightContain(String filterName, String item);

    /**
     * Deletes an item from the Cuckoo filter (CF.DEL).
     * This operation is unique to Cuckoo filters — Bloom filters do not support deletion.
     *
     * @param filterName the name of the Cuckoo filter
     * @param item       the item to delete
     * @return true if the item was found and deleted
     */
    boolean delete(String filterName, String item);
}
