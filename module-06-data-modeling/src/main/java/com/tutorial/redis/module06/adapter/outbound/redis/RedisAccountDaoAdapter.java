package com.tutorial.redis.module06.adapter.outbound.redis;

import com.tutorial.redis.module06.domain.model.AccountAggregate;
import com.tutorial.redis.module06.domain.port.outbound.AccountDaoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Redis adapter implementing the Account DAO using the Hash-per-entity pattern.
 * Each account is stored as a Redis Hash with individual fields (holderName, balance,
 * currency, createdAt, status), enabling partial reads and atomic field updates.
 *
 * <p>Secondary indexes are maintained as Redis Sets for currency and status lookups.</p>
 *
 * <h3>Key Schema</h3>
 * <ul>
 *   <li>Entity: {@code banking:account:{accountId}} (HASH)</li>
 *   <li>Currency index: {@code idx:account:currency:{currency}} (SET of accountIds)</li>
 *   <li>Status index: {@code idx:account:status:{status}} (SET of accountIds)</li>
 * </ul>
 */
@Component
public class RedisAccountDaoAdapter implements AccountDaoPort {

    private static final Logger log = LoggerFactory.getLogger(RedisAccountDaoAdapter.class);

    private static final String ENTITY_PREFIX = "banking:account:";
    private static final String CURRENCY_INDEX_PREFIX = "idx:account:currency:";
    private static final String STATUS_INDEX_PREFIX = "idx:account:status:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisAccountDaoAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Saves an account as a Redis Hash and adds its ID to the currency and status
     * secondary index Sets. Uses {@code HSET} for the entity and {@code SADD}
     * for the indexes.
     */
    @Override
    public void save(AccountAggregate account) {
        String key = ENTITY_PREFIX + account.getAccountId();

        Map<String, String> fields = new HashMap<>();
        fields.put("accountId", account.getAccountId());
        fields.put("holderName", account.getHolderName());
        fields.put("balance", String.valueOf(account.getBalance()));
        fields.put("currency", account.getCurrency());
        fields.put("createdAt", account.getCreatedAt().toString());
        fields.put("status", account.getStatus());

        stringRedisTemplate.opsForHash().putAll(key, fields);

        String currencyIndexKey = CURRENCY_INDEX_PREFIX + account.getCurrency();
        stringRedisTemplate.opsForSet().add(currencyIndexKey, account.getAccountId());

        String statusIndexKey = STATUS_INDEX_PREFIX + account.getStatus();
        stringRedisTemplate.opsForSet().add(statusIndexKey, account.getAccountId());

        log.debug("Saved account {} as Hash with currency index [{}] and status index [{}]",
                account.getAccountId(), currencyIndexKey, statusIndexKey);
    }

    /**
     * Retrieves an account by performing {@code HGETALL} on the entity key
     * and reconstructing the {@link AccountAggregate} from the hash fields.
     */
    @Override
    public Optional<AccountAggregate> findById(String accountId) {
        String key = ENTITY_PREFIX + accountId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) {
            log.debug("Account {} not found", accountId);
            return Optional.empty();
        }

        AccountAggregate account = mapToAccount(entries);
        log.debug("Found account {}", accountId);
        return Optional.of(account);
    }

    /**
     * Deletes an account by first reading it to determine its currency and status
     * (for index cleanup), then removing the account ID from both secondary index
     * Sets, and finally deleting the entity Hash key.
     */
    @Override
    public void delete(String accountId) {
        String key = ENTITY_PREFIX + accountId;

        // Read account first to determine index keys for cleanup
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (!entries.isEmpty()) {
            String currency = (String) entries.get("currency");
            String status = (String) entries.get("status");

            // Remove from secondary indexes
            if (currency != null) {
                stringRedisTemplate.opsForSet().remove(CURRENCY_INDEX_PREFIX + currency, accountId);
            }
            if (status != null) {
                stringRedisTemplate.opsForSet().remove(STATUS_INDEX_PREFIX + status, accountId);
            }
        }

        stringRedisTemplate.delete(key);
        log.debug("Deleted account {}", accountId);
    }

    /**
     * Finds all accounts denominated in the given currency by reading the
     * {@code idx:account:currency:{currency}} Set, then fetching each account
     * by its ID via {@code HGETALL}.
     */
    @Override
    public List<AccountAggregate> findByCurrency(String currency) {
        String indexKey = CURRENCY_INDEX_PREFIX + currency;
        Set<String> accountIds = stringRedisTemplate.opsForSet().members(indexKey);

        if (accountIds == null || accountIds.isEmpty()) {
            log.debug("No accounts found for currency {}", currency);
            return List.of();
        }

        List<AccountAggregate> accounts = new ArrayList<>();
        for (String accountId : accountIds) {
            findById(accountId).ifPresent(accounts::add);
        }

        log.debug("Found {} accounts for currency {}", accounts.size(), currency);
        return accounts;
    }

    /**
     * Finds all accounts with the given status by reading the
     * {@code idx:account:status:{status}} Set, then fetching each account
     * by its ID via {@code HGETALL}.
     */
    @Override
    public List<AccountAggregate> findByStatus(String status) {
        String indexKey = STATUS_INDEX_PREFIX + status;
        Set<String> accountIds = stringRedisTemplate.opsForSet().members(indexKey);

        if (accountIds == null || accountIds.isEmpty()) {
            log.debug("No accounts found for status {}", status);
            return List.of();
        }

        List<AccountAggregate> accounts = new ArrayList<>();
        for (String accountId : accountIds) {
            findById(accountId).ifPresent(accounts::add);
        }

        log.debug("Found {} accounts for status {}", accounts.size(), status);
        return accounts;
    }

    /**
     * Reconstructs an {@link AccountAggregate} from a Redis Hash entry map.
     */
    private AccountAggregate mapToAccount(Map<Object, Object> entries) {
        return new AccountAggregate(
                (String) entries.get("accountId"),
                (String) entries.get("holderName"),
                Double.parseDouble((String) entries.get("balance")),
                (String) entries.get("currency"),
                Instant.parse((String) entries.get("createdAt")),
                (String) entries.get("status")
        );
    }
}
