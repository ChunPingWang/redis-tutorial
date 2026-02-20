package com.tutorial.redis.module07.domain.model;

import java.util.Objects;

/**
 * Represents the reconstructed state of an account derived from
 * replaying a sequence of {@link AccountEvent}s.
 *
 * <p>This is the read-model projection in the event sourcing pattern.
 * Instead of persisting the current state directly, the state is
 * rebuilt by folding over all historical events via {@link #apply(AccountEvent)}.</p>
 *
 * <p>Account statuses:</p>
 * <ul>
 *   <li>{@code ACTIVE} — account is open and operational</li>
 *   <li>{@code FROZEN} — account is frozen; no withdrawals or deposits</li>
 * </ul>
 *
 * Immutable value object — {@code apply()} returns a new instance.
 */
public class AccountState {

    private final String accountId;
    private final double balance;
    private final String status;
    private final int eventCount;

    public AccountState(String accountId, double balance, String status, int eventCount) {
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.balance = balance;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.eventCount = eventCount;
    }

    public String getAccountId() { return accountId; }
    public double getBalance() { return balance; }
    public String getStatus() { return status; }
    public int getEventCount() { return eventCount; }

    /**
     * Applies a single event to produce a new {@link AccountState}.
     * This method implements the event replay fold:
     * <ul>
     *   <li>{@code ACCOUNT_OPENED} — sets status to ACTIVE</li>
     *   <li>{@code MONEY_DEPOSITED} — adds amount to balance</li>
     *   <li>{@code MONEY_WITHDRAWN} — subtracts amount from balance</li>
     *   <li>{@code ACCOUNT_FROZEN} — sets status to FROZEN</li>
     * </ul>
     *
     * @param event the account event to apply
     * @return a new AccountState reflecting the event
     */
    public AccountState apply(AccountEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        int newEventCount = this.eventCount + 1;

        return switch (event.getEventType()) {
            case "ACCOUNT_OPENED" -> new AccountState(this.accountId, this.balance, "ACTIVE", newEventCount);
            case "MONEY_DEPOSITED" -> {
                double depositAmount = event.getAmount() != null ? event.getAmount() : 0.0;
                yield new AccountState(this.accountId, this.balance + depositAmount, this.status, newEventCount);
            }
            case "MONEY_WITHDRAWN" -> {
                double withdrawAmount = event.getAmount() != null ? event.getAmount() : 0.0;
                yield new AccountState(this.accountId, this.balance - withdrawAmount, this.status, newEventCount);
            }
            case "ACCOUNT_FROZEN" -> new AccountState(this.accountId, this.balance, "FROZEN", newEventCount);
            default -> new AccountState(this.accountId, this.balance, this.status, newEventCount);
        };
    }

    /**
     * Creates the initial state for an account before any events are applied.
     *
     * @param accountId the account identifier
     * @return an initial AccountState with zero balance, ACTIVE status, and zero events
     */
    public static AccountState initial(String accountId) {
        return new AccountState(accountId, 0.0, "ACTIVE", 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountState that)) return false;
        return accountId.equals(that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }

    @Override
    public String toString() {
        return "AccountState{accountId='%s', balance=%.2f, status='%s', eventCount=%d}".formatted(
                accountId, balance, status, eventCount);
    }
}
