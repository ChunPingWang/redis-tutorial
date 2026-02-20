package com.tutorial.redis.module05.domain.port.inbound;

/**
 * Inbound port: optimistic-lock-based balance updates with retry logic.
 * Uses Redis WATCH + MULTI/EXEC to implement compare-and-set (CAS) semantics,
 * retrying automatically on concurrent modification up to a maximum number of attempts.
 */
public interface OptimisticBalanceUpdateUseCase {

    /**
     * Attempts to update an account balance using CAS with automatic retry.
     * The operation WATCHes the account key, compares the current balance
     * against the expected value, and sets the new balance inside a MULTI/EXEC block.
     * If a concurrent modification is detected (EXEC returns null), the operation
     * is retried up to {@code maxRetries} times.
     *
     * @param accountId       the account to update
     * @param expectedBalance the expected current balance
     * @param newBalance      the desired new balance
     * @param maxRetries      the maximum number of retry attempts
     * @return true if the update succeeded within the allowed retries, false otherwise
     */
    boolean updateBalanceWithRetry(String accountId, double expectedBalance, double newBalance, int maxRetries);
}
