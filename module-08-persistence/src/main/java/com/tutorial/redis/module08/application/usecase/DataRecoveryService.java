package com.tutorial.redis.module08.application.usecase;

import com.tutorial.redis.module08.domain.model.RecoveryResult;
import com.tutorial.redis.module08.domain.port.inbound.DataRecoveryUseCase;
import com.tutorial.redis.module08.domain.port.outbound.DataRecoveryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service for simulating data recovery scenarios.
 *
 * <p>Implements the {@link DataRecoveryUseCase} inbound port. Orchestrates
 * writing test data, counting surviving keys, and measuring recovery time
 * to produce a {@link RecoveryResult} that quantifies data durability.</p>
 *
 * <p>The recovery simulation writes a known number of keys and then
 * immediately counts them back. The count operation is timed as a simple
 * proxy for recovery duration measurement.</p>
 */
@Service
public class DataRecoveryService implements DataRecoveryUseCase {

    private static final Logger log = LoggerFactory.getLogger(DataRecoveryService.class);

    private final DataRecoveryPort dataRecoveryPort;

    public DataRecoveryService(DataRecoveryPort dataRecoveryPort) {
        this.dataRecoveryPort = dataRecoveryPort;
    }

    /**
     * Simulates a data recovery scenario by writing test keys, counting
     * how many survive, and measuring the time taken.
     *
     * <p>Steps:
     * <ol>
     *   <li>Write {@code keyCount} test keys with the given prefix</li>
     *   <li>Count keys matching the prefix (timed as recovery proxy)</li>
     *   <li>Build and return a {@link RecoveryResult} with metrics</li>
     * </ol>
     *
     * @param keyPrefix the prefix used for test keys
     * @param keyCount  the number of test keys to write
     * @return a {@link RecoveryResult} with recovery metrics
     */
    @Override
    public RecoveryResult simulateRecovery(String keyPrefix, int keyCount) {
        log.info("Simulating recovery: writing {} keys with prefix '{}'", keyCount, keyPrefix);

        // Step 1: Write test data
        dataRecoveryPort.writeTestData(keyPrefix, keyCount);

        // Step 2: Count keys and measure recovery time
        long startNanos = System.nanoTime();
        int keysRecovered = dataRecoveryPort.countKeys(keyPrefix);
        long elapsedNanos = System.nanoTime() - startNanos;
        long recoveryTimeMs = elapsedNanos / 1_000_000;

        // Step 3: Build result
        RecoveryResult result = new RecoveryResult(
                "simulated",
                keyCount,
                keysRecovered,
                recoveryTimeMs
        );

        log.info("Recovery simulation complete: {}", result);
        return result;
    }
}
