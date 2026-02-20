package com.tutorial.redis.module07.application.usecase;

import com.tutorial.redis.module07.domain.model.AccountEvent;
import com.tutorial.redis.module07.domain.model.AccountState;
import com.tutorial.redis.module07.domain.port.inbound.EventSourcingUseCase;
import com.tutorial.redis.module07.domain.port.outbound.EventStorePort;
import com.tutorial.redis.module07.domain.service.EventReplayService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service for event sourcing operations on account domain events.
 *
 * <p>Implements the {@link EventSourcingUseCase} inbound port by coordinating
 * between the {@link EventStorePort} (for persistence) and the
 * {@link EventReplayService} (for state reconstruction). Event streams are
 * keyed by {@code event:account:{accountId}}.</p>
 *
 * <p>This service orchestrates three core event sourcing operations:</p>
 * <ul>
 *   <li>Append — stores a new domain event to the account's event stream</li>
 *   <li>Replay — reads all events and folds them into the current account state</li>
 *   <li>Get events — retrieves the full event history for an account</li>
 * </ul>
 */
@Service
public class EventSourcingService implements EventSourcingUseCase {

    private static final String STREAM_KEY_PREFIX = "event:account:";

    private final EventStorePort eventStorePort;
    private final EventReplayService eventReplayService;

    public EventSourcingService(EventStorePort eventStorePort,
                                EventReplayService eventReplayService) {
        this.eventStorePort = eventStorePort;
        this.eventReplayService = eventReplayService;
    }

    /**
     * Appends an account event to the event store.
     * The stream key is derived from the event's account ID.
     *
     * @param event the account event to append
     * @return the auto-generated event ID
     */
    @Override
    public String appendAccountEvent(AccountEvent event) {
        String streamKey = STREAM_KEY_PREFIX + event.getAccountId();
        return eventStorePort.appendEvent(streamKey, event);
    }

    /**
     * Replays all events for the given account to reconstruct its current state.
     * Reads the full event history from the store and delegates to
     * {@link EventReplayService#replay(List)} for state folding.
     *
     * @param accountId the account identifier
     * @return the reconstructed account state
     */
    @Override
    public AccountState replayEvents(String accountId) {
        String streamKey = STREAM_KEY_PREFIX + accountId;
        List<AccountEvent> events = eventStorePort.readAllEvents(streamKey);
        return eventReplayService.replay(events);
    }

    /**
     * Retrieves all events for the given account in chronological order.
     *
     * @param accountId the account identifier
     * @return a list of account events
     */
    @Override
    public List<AccountEvent> getEvents(String accountId) {
        String streamKey = STREAM_KEY_PREFIX + accountId;
        return eventStorePort.readAllEvents(streamKey);
    }
}
