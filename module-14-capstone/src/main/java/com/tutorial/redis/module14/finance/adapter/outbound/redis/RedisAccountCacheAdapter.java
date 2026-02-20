package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.module14.finance.domain.port.outbound.AccountCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Redis adapter for account caching operations.
 *
 * <p>Implements {@link AccountCachePort} using:</p>
 * <ul>
 *   <li>Redis Strings for balance caching ({@code SET/GET})</li>
 *   <li>RedisJSON via Lua scripts for profile storage ({@code JSON.SET/JSON.GET})</li>
 * </ul>
 */
@Component
public class RedisAccountCacheAdapter implements AccountCachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisAccountCacheAdapter.class);

    private static final String BALANCE_KEY_PREFIX = "finance:account:balance:";
    private static final String PROFILE_KEY_PREFIX = "finance:account:profile:";

    /**
     * Lua script for JSON.SET: stores a JSON document at the root path.
     */
    private static final DefaultRedisScript<String> JSON_SET_SCRIPT = new DefaultRedisScript<>(
            "redis.call('JSON.SET', KEYS[1], '$', ARGV[1])\n"
                    + "return 'OK'",
            String.class);

    /**
     * Lua script for JSON.GET: retrieves a JSON document from the root path.
     */
    private static final DefaultRedisScript<String> JSON_GET_SCRIPT = new DefaultRedisScript<>(
            "return redis.call('JSON.GET', KEYS[1], '$')",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisAccountCacheAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void setBalance(String accountId, double balance) {
        String key = BALANCE_KEY_PREFIX + accountId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(balance));
        log.debug("Set balance for account {}: {}", accountId, balance);
    }

    @Override
    public Double getBalance(String accountId) {
        String key = BALANCE_KEY_PREFIX + accountId;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            log.debug("No cached balance for account {}", accountId);
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid balance value for account {}: {}", accountId, value);
            return null;
        }
    }

    @Override
    public void storeProfile(String accountId, String jsonProfile) {
        String key = PROFILE_KEY_PREFIX + accountId;
        List<String> keys = Collections.singletonList(key);
        stringRedisTemplate.execute(JSON_SET_SCRIPT, keys, jsonProfile);
        log.debug("Stored JSON profile for account {}", accountId);
    }

    @Override
    public String getProfile(String accountId) {
        String key = PROFILE_KEY_PREFIX + accountId;
        List<String> keys = Collections.singletonList(key);
        String result = stringRedisTemplate.execute(JSON_GET_SCRIPT, keys);
        log.debug("Retrieved JSON profile for account {}: {}", accountId, result);
        return result;
    }
}
