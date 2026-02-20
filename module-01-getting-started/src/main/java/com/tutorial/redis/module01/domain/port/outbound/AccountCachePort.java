package com.tutorial.redis.module01.domain.port.outbound;

import com.tutorial.redis.module01.domain.model.Account;
import java.time.Duration;
import java.util.Optional;

/**
 * Outbound port for Account cache operations.
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface AccountCachePort {

    void save(Account account, Duration ttl);

    Optional<Account> findById(String accountId);

    void evict(String accountId);

    boolean exists(String accountId);
}
