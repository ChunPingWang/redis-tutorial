package com.tutorial.redis.module07.domain.port.outbound;

import com.tutorial.redis.module07.domain.model.AccountEvent;

import java.util.List;

/**
 * Outbound port for event sourcing storage using Redis Streams.
 * Appends domain events to a stream and reads them back for replay.
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface EventStorePort {

    /**
     * Appends an account event to the specified event stream.
     * The event fields are serialized into the stream entry payload.
     *
     * @param streamKey the stream key (typically "account-events:{accountId}")
     * @param event     the account event to append
     * @return the auto-generated event ID assigned by Redis
     */
    String appendEvent(String streamKey, AccountEvent event);

    /**
     * Reads all events from the specified event stream.
     * Used for full state reconstruction via event replay.
     *
     * @param streamKey the stream key
     * @return all account events in chronological order
     */
    List<AccountEvent> readAllEvents(String streamKey);

    /**
     * Reads events from the specified stream starting after the given event ID.
     * Used for partial replay / catch-up scenarios.
     *
     * @param streamKey   the stream key
     * @param fromEventId the event ID to read after (exclusive)
     * @return account events after the given ID in chronological order
     */
    List<AccountEvent> readEventsFrom(String streamKey, String fromEventId);
}
