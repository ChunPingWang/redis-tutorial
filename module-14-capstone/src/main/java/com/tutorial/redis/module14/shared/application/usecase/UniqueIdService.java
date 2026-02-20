package com.tutorial.redis.module14.shared.application.usecase;

import com.tutorial.redis.module14.shared.domain.model.UniqueId;
import com.tutorial.redis.module14.shared.domain.port.outbound.UniqueIdPort;
import org.springframework.stereotype.Service;

/**
 * Application service for distributed unique ID generation.
 *
 * <p>Combines a Redis-backed atomic counter with the current timestamp
 * and a user-defined prefix to generate globally unique, sortable IDs
 * without coordination between application instances.</p>
 */
@Service
public class UniqueIdService {

    private final UniqueIdPort uniqueIdPort;

    public UniqueIdService(UniqueIdPort uniqueIdPort) {
        this.uniqueIdPort = uniqueIdPort;
    }

    /**
     * Generates a new unique ID with the given prefix.
     *
     * @param prefix the prefix for the generated ID
     * @return a new {@link UniqueId} with sequence, timestamp, and prefix
     */
    public UniqueId generateId(String prefix) {
        long sequence = uniqueIdPort.nextSequence("uid:" + prefix);
        long timestamp = System.currentTimeMillis();
        return new UniqueId(prefix, sequence, timestamp);
    }
}
