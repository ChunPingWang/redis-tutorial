package com.tutorial.redis.module14.finance.domain.port.outbound;

/**
 * Outbound port for fraud detection operations.
 *
 * <p>Abstracts a Redis Bloom filter used to efficiently check whether
 * a transaction has already been flagged for fraud. Bloom filters provide
 * space-efficient probabilistic membership testing with zero false negatives.</p>
 */
public interface FraudDetectionPort {

    /**
     * Adds a transaction identifier to the Bloom filter.
     *
     * @param txId the transaction identifier to add
     */
    void addToBloomFilter(String txId);

    /**
     * Checks whether a transaction identifier might exist in the Bloom filter.
     *
     * @param txId the transaction identifier to check
     * @return {@code true} if the item might exist (possible false positive),
     *         {@code false} if it definitely does not exist
     */
    boolean mightExist(String txId);
}
