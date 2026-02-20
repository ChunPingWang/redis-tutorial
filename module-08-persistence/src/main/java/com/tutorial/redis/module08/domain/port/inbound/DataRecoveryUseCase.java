package com.tutorial.redis.module08.domain.port.inbound;

import com.tutorial.redis.module08.domain.model.RecoveryResult;

/**
 * Inbound port: simulating data recovery for a persistence strategy.
 *
 * <p>Writes test data, then counts how many keys survive after a persistence
 * cycle to produce a {@link RecoveryResult} that quantifies data durability
 * and recovery speed.</p>
 */
public interface DataRecoveryUseCase {

    /**
     * Simulates a data recovery scenario by writing test keys, triggering
     * persistence, and counting how many keys are recoverable.
     *
     * @param keyPrefix the prefix used for test keys
     * @param keyCount  the number of test keys to write
     * @return a {@link RecoveryResult} with recovery metrics
     */
    RecoveryResult simulateRecovery(String keyPrefix, int keyCount);
}
