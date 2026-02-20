package com.tutorial.redis.module05.application.usecase;

import com.tutorial.redis.module05.domain.port.inbound.OptimisticBalanceUpdateUseCase;
import com.tutorial.redis.module05.domain.port.outbound.OptimisticLockPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service for optimistic-lock-based balance updates with retry logic.
 * Uses the {@link OptimisticLockPort} to perform compare-and-set (CAS) operations
 * backed by Redis WATCH + MULTI/EXEC, and retries automatically on concurrent
 * modification up to a configurable maximum number of attempts.
 */
@Service
public class OptimisticBalanceUpdateService implements OptimisticBalanceUpdateUseCase {

    private static final Logger log = LoggerFactory.getLogger(OptimisticBalanceUpdateService.class);

    private final OptimisticLockPort optimisticLockPort;

    public OptimisticBalanceUpdateService(OptimisticLockPort optimisticLockPort) {
        this.optimisticLockPort = optimisticLockPort;
    }

    /**
     * Attempts to update an account balance using CAS with automatic retry.
     * Each attempt WATCHes the key, compares the current balance against the
     * expected value, and sets the new balance inside a MULTI/EXEC block.
     * If a concurrent modification is detected (EXEC returns null), the operation
     * is retried up to {@code maxRetries} times.
     *
     * @param accountId       the account to update
     * @param expectedBalance the expected current balance
     * @param newBalance      the desired new balance
     * @param maxRetries      the maximum number of retry attempts
     * @return true if the update succeeded within the allowed retries, false otherwise
     */
    @Override
    public boolean updateBalanceWithRetry(String accountId, double expectedBalance,
                                          double newBalance, int maxRetries) {
        log.debug("Attempting CAS balance update for account {} (maxRetries={})",
                accountId, maxRetries);

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                log.debug("Retry attempt {} for CAS balance update on account {}", attempt, accountId);
            }

            boolean success = optimisticLockPort.compareAndSetBalance(accountId, expectedBalance, newBalance);
            if (success) {
                log.debug("CAS balance update succeeded for account {} on attempt {}",
                        accountId, attempt + 1);
                return true;
            }
        }

        log.debug("CAS balance update failed for account {} after {} retries",
                accountId, maxRetries);
        return false;
    }
}
