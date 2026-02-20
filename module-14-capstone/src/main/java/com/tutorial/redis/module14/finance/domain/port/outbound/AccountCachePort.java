package com.tutorial.redis.module14.finance.domain.port.outbound;

/**
 * Outbound port for account caching operations.
 *
 * <p>Abstracts the storage of account balances (Redis Strings) and
 * account profiles (RedisJSON) behind a port interface, allowing
 * the application layer to remain infrastructure-agnostic.</p>
 */
public interface AccountCachePort {

    /**
     * Stores an account balance as a Redis String.
     *
     * @param accountId the unique identifier of the account
     * @param balance   the balance to store
     */
    void setBalance(String accountId, double balance);

    /**
     * Retrieves the cached account balance.
     *
     * @param accountId the unique identifier of the account
     * @return the balance, or {@code null} if not cached
     */
    Double getBalance(String accountId);

    /**
     * Stores an account profile as a JSON document via {@code JSON.SET}.
     *
     * @param accountId   the unique identifier of the account
     * @param jsonProfile the JSON string representing the account profile
     */
    void storeProfile(String accountId, String jsonProfile);

    /**
     * Retrieves an account profile JSON document via {@code JSON.GET}.
     *
     * @param accountId the unique identifier of the account
     * @return the JSON string, or {@code null} if not found
     */
    String getProfile(String accountId);
}
