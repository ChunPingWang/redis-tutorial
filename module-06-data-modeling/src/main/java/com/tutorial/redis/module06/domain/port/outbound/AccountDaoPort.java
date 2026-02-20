package com.tutorial.redis.module06.domain.port.outbound;

import com.tutorial.redis.module06.domain.model.AccountAggregate;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for Account DAO operations.
 * Aligned with the RU102J SiteDaoRedisImpl pattern: full CRUD plus
 * secondary-index-based lookups.
 *
 * Implementations may choose any of the three aggregate storage strategies:
 * JSON String, Hash per entity, or Multi-Key decomposition.
 *
 * Secondary indexes:
 * - Set-based index by currency for {@link #findByCurrency(String)}
 * - Set-based index by status for {@link #findByStatus(String)}
 */
public interface AccountDaoPort {

    void save(AccountAggregate account);

    Optional<AccountAggregate> findById(String accountId);

    void delete(String accountId);

    /**
     * Finds all accounts with the given currency using a Set-based secondary index.
     */
    List<AccountAggregate> findByCurrency(String currency);

    /**
     * Finds all accounts with the given status using a Set-based secondary index.
     */
    List<AccountAggregate> findByStatus(String status);
}
