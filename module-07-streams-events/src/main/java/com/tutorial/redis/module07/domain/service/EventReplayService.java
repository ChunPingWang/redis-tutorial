package com.tutorial.redis.module07.domain.service;

import com.tutorial.redis.module07.domain.model.AccountEvent;
import com.tutorial.redis.module07.domain.model.AccountState;

import java.util.List;
import java.util.Objects;

/**
 * Domain service for event replay logic.
 * Pure domain logic — zero framework dependency.
 *
 * <p>Reconstructs an {@link AccountState} by folding over a chronological
 * sequence of {@link AccountEvent}s. Starting from an initial state
 * (balance=0, status=ACTIVE, eventCount=0), each event is applied
 * in order to produce the final projected state.</p>
 *
 * <p>Event handling rules:</p>
 * <ul>
 *   <li>{@code ACCOUNT_OPENED} — sets status to ACTIVE</li>
 *   <li>{@code MONEY_DEPOSITED} — adds amount to balance</li>
 *   <li>{@code MONEY_WITHDRAWN} — subtracts amount from balance</li>
 *   <li>{@code ACCOUNT_FROZEN} — sets status to FROZEN</li>
 * </ul>
 */
public class EventReplayService {

    /**
     * Replays a list of account events to reconstruct the account state.
     * Events are applied in list order (assumed chronological).
     *
     * <p>If the event list is empty, returns the initial state with
     * the account ID derived from the first event. For a truly empty
     * list, an {@link IllegalArgumentException} is thrown since
     * no account ID can be determined.</p>
     *
     * @param events the chronological list of account events (must not be null or empty)
     * @return the reconstructed account state after applying all events
     * @throws IllegalArgumentException if events is empty
     */
    public AccountState replay(List<AccountEvent> events) {
        Objects.requireNonNull(events, "events must not be null");
        if (events.isEmpty()) {
            throw new IllegalArgumentException("events must not be empty");
        }

        String accountId = events.getFirst().getAccountId();
        AccountState state = AccountState.initial(accountId);

        for (AccountEvent event : events) {
            state = state.apply(event);
        }

        return state;
    }
}
