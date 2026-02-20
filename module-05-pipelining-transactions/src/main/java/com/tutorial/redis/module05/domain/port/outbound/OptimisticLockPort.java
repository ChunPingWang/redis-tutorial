package com.tutorial.redis.module05.domain.port.outbound;

/**
 * Outbound port for Redis WATCH-based optimistic locking operations.
 * Uses WATCH + MULTI/EXEC to implement compare-and-set (CAS) semantics,
 * allowing safe concurrent updates to account balances.
 * Implemented by a Redis adapter in the infrastructure layer.
 */
public interface OptimisticLockPort {

    /**
     * Performs a compare-and-set update on an account balance using
     * Redis WATCH + MULTI/EXEC for optimistic locking.
     * The operation succeeds only if the current balance matches the expected value
     * at the time of execution (i.e., no concurrent modification occurred).
     *
     * @param accountId       the account to update
     * @param expectedBalance the expected current balance (used for the compare step)
     * @param newBalance      the new balance to set if the expected balance matches
     * @return true if the CAS operation succeeded, false if aborted due to concurrent modification
     */
    boolean compareAndSetBalance(String accountId, double expectedBalance, double newBalance);
}
