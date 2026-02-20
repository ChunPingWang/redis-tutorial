package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.module14.finance.domain.model.Transaction;
import com.tutorial.redis.module14.finance.domain.port.outbound.TransactionSearchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis adapter for transaction search operations.
 *
 * <p>Implements {@link TransactionSearchPort} using Redis hashes for
 * storage and RediSearch (FT.CREATE / FT.SEARCH) via Lua scripts for
 * indexing and querying transactions.</p>
 *
 * <p>On first {@link #indexTransaction} call, the adapter creates the
 * RediSearch index {@code idx:finance-tx} if it does not already exist,
 * silently ignoring the "Index already exists" error.</p>
 */
@Component
public class RedisTransactionSearchAdapter implements TransactionSearchPort {

    private static final Logger log = LoggerFactory.getLogger(RedisTransactionSearchAdapter.class);

    private static final String TX_KEY_PREFIX = "finance:tx:";
    private static final String INDEX_NAME = "idx:finance-tx";

    /**
     * Lua script to create the RediSearch index.
     * Returns "OK" on success or "EXISTS" if the index already exists.
     */
    private static final DefaultRedisScript<String> CREATE_INDEX_SCRIPT = new DefaultRedisScript<>(
            "local ok, err = pcall(redis.call, 'FT.CREATE', '" + INDEX_NAME + "', "
                    + "'ON', 'HASH', "
                    + "'PREFIX', '1', '" + TX_KEY_PREFIX + "', "
                    + "'SCHEMA', "
                    + "'fromAccountId', 'TAG', "
                    + "'toAccountId', 'TAG', "
                    + "'amount', 'NUMERIC', 'SORTABLE', "
                    + "'currency', 'TAG', "
                    + "'status', 'TAG')\n"
                    + "if ok then return 'OK' else return 'EXISTS' end",
            String.class);

    /**
     * Lua script to search the transaction index.
     * Returns JSON-encoded results from FT.SEARCH.
     */
    private static final DefaultRedisScript<String> SEARCH_SCRIPT = new DefaultRedisScript<>(
            "local result = redis.call('FT.SEARCH', '" + INDEX_NAME + "', ARGV[1], 'LIMIT', '0', '10')\n"
                    + "return cjson.encode(result)",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;
    private volatile boolean indexCreated = false;

    public RedisTransactionSearchAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void indexTransaction(Transaction tx) {
        ensureIndexExists();

        String key = TX_KEY_PREFIX + tx.getTransactionId();
        Map<String, String> fields = new HashMap<>();
        fields.put("fromAccountId", tx.getFromAccountId());
        fields.put("toAccountId", tx.getToAccountId());
        fields.put("amount", String.valueOf(tx.getAmount()));
        fields.put("currency", tx.getCurrency());
        fields.put("status", tx.getStatus());
        fields.put("timestamp", String.valueOf(tx.getTimestamp()));

        stringRedisTemplate.opsForHash().putAll(key, fields);
        log.debug("Indexed transaction {}", tx.getTransactionId());
    }

    @Override
    public List<String> search(String query) {
        try {
            String json = stringRedisTemplate.execute(
                    SEARCH_SCRIPT, Collections.emptyList(), query);
            log.debug("Search result for '{}': {}", query, json);
            if (json == null || json.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> results = new ArrayList<>();
            results.add(json);
            return results;
        } catch (Exception e) {
            log.warn("FT.SEARCH failed for query '{}': {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Ensures the RediSearch index exists, creating it if necessary.
     * Uses double-checked locking for thread safety.
     */
    private void ensureIndexExists() {
        if (!indexCreated) {
            synchronized (this) {
                if (!indexCreated) {
                    try {
                        String result = stringRedisTemplate.execute(
                                CREATE_INDEX_SCRIPT, Collections.emptyList());
                        log.info("FT.CREATE result: {}", result);
                    } catch (Exception e) {
                        log.debug("Index creation returned: {}", e.getMessage());
                    }
                    indexCreated = true;
                }
            }
        }
    }
}
