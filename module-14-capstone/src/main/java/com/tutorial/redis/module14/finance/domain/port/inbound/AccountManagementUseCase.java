package com.tutorial.redis.module14.finance.domain.port.inbound;

import com.tutorial.redis.module14.finance.domain.model.Account;

/**
 * Inbound port for account management operations.
 *
 * <p>Defines use cases for caching account balances using Redis Strings
 * and storing full account profiles using RedisJSON. This port is
 * implemented by the application service and invoked by controllers.</p>
 */
public interface AccountManagementUseCase {

    /**
     * Caches the account balance in Redis for fast retrieval.
     *
     * @param accountId the unique identifier of the account
     * @param balance   the current balance to cache
     */
    void cacheAccountBalance(String accountId, double balance);

    /**
     * Retrieves the cached account balance from Redis.
     *
     * @param accountId the unique identifier of the account
     * @return the cached balance, or {@code null} if not found
     */
    Double getAccountBalance(String accountId);

    /**
     * Stores the full account profile as a JSON document in Redis.
     *
     * @param account the account to store
     */
    void storeAccountProfile(Account account);

    /**
     * Retrieves the full account profile from Redis.
     *
     * @param accountId the unique identifier of the account
     * @return the account, or {@code null} if not found
     */
    Account getAccountProfile(String accountId);
}
