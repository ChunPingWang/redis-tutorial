package com.tutorial.redis.module09.domain.port.inbound;

import com.tutorial.redis.module09.domain.model.FailoverEvent;

import java.util.List;

/**
 * Inbound port: simulating and verifying failover scenarios.
 *
 * <p>Provides the application-level API for generating an educational
 * failover event sequence and verifying data integrity after a
 * simulated failover.</p>
 */
public interface FailoverSimulationUseCase {

    /**
     * Simulates the Sentinel failover process and returns an ordered
     * sequence of failover events describing each step.
     *
     * @return a list of {@link FailoverEvent} in chronological order
     */
    List<FailoverEvent> simulateFailoverSequence();

    /**
     * Verifies data integrity after a simulated failover by checking how
     * many keys with the given prefix survived the transition.
     *
     * @param keyPrefix     the prefix of keys to verify
     * @param expectedCount the expected total number of keys
     * @return the number of keys that were successfully read back
     */
    int verifyDataAfterFailover(String keyPrefix, int expectedCount);
}
