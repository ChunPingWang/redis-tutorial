package com.tutorial.redis.module07.domain.port.inbound;

import com.tutorial.redis.module07.domain.model.AccountEvent;
import com.tutorial.redis.module07.domain.model.AccountState;

import java.util.List;

/**
 * Inbound port: event sourcing operations for account domain events.
 * Supports appending events, replaying to reconstruct state,
 * and retrieving the event history.
 */
public interface EventSourcingUseCase {

    /**
     * Appends an account event to the event store.
     *
     * @param event the account event to append
     * @return the auto-generated event ID
     */
    String appendAccountEvent(AccountEvent event);

    /**
     * Replays all events for the given account to reconstruct its current state.
     *
     * @param accountId the account identifier
     * @return the reconstructed account state
     */
    AccountState replayEvents(String accountId);

    /**
     * Retrieves all events for the given account in chronological order.
     *
     * @param accountId the account identifier
     * @return a list of account events
     */
    List<AccountEvent> getEvents(String accountId);
}
