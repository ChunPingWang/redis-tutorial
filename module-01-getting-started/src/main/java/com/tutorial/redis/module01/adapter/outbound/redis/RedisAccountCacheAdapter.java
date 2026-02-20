package com.tutorial.redis.module01.adapter.outbound.redis;

import com.tutorial.redis.common.config.RedisKeyConvention;
import com.tutorial.redis.module01.domain.model.Account;
import com.tutorial.redis.module01.domain.port.outbound.AccountCachePort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisAccountCacheAdapter implements AccountCachePort {

    private static final String SERVICE = "banking";
    private static final String ENTITY = "account";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisAccountCacheAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(Account account, Duration ttl) {
        String key = buildKey(account.getAccountId());
        redisTemplate.opsForValue().set(key, account, ttl);
    }

    @Override
    public Optional<Account> findById(String accountId) {
        String key = buildKey(accountId);
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Account account) {
            return Optional.of(account);
        }
        return Optional.empty();
    }

    @Override
    public void evict(String accountId) {
        redisTemplate.delete(buildKey(accountId));
    }

    @Override
    public boolean exists(String accountId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(accountId)));
    }

    private String buildKey(String accountId) {
        return RedisKeyConvention.buildKey(SERVICE, ENTITY, accountId);
    }
}
