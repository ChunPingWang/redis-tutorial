package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.module04.domain.model.TransactionEvent;
import com.tutorial.redis.module04.domain.port.outbound.TransactionPersistencePort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory persistence adapter simulating a database for transaction events.
 * Stores persisted events in an internal list for test verification.
 */
@Component
public class InMemoryTransactionPersistence implements TransactionPersistencePort {

    private final List<TransactionEvent> persistedEvents =
            Collections.synchronizedList(new ArrayList<>());

    @Override
    public void saveAll(List<TransactionEvent> events) {
        persistedEvents.addAll(events);
    }

    /**
     * Returns an unmodifiable view of all persisted transaction events.
     * Used for test verification of the Write-Behind pattern.
     *
     * @return the list of persisted events
     */
    public List<TransactionEvent> getPersistedEvents() {
        return Collections.unmodifiableList(persistedEvents);
    }
}
