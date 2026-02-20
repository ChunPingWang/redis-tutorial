package com.tutorial.redis.module06.domain.port.inbound;

import com.tutorial.redis.module06.domain.model.AccountAggregate;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: manage accounts using the DAO pattern with Redis.
 * Supports CRUD operations and secondary-index-based queries
 * by currency and status.
 */
public interface ManageAccountUseCase {

    void createAccount(AccountAggregate account);

    Optional<AccountAggregate> getAccount(String accountId);

    void deleteAccount(String accountId);

    /**
     * Finds all accounts denominated in the given currency.
     */
    List<AccountAggregate> findAccountsByCurrency(String currency);

    /**
     * Finds all accounts with the given status (ACTIVE, FROZEN, CLOSED).
     */
    List<AccountAggregate> findAccountsByStatus(String status);
}
