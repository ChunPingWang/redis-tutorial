package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.common.config.RedisKeyConvention;
import com.tutorial.redis.module04.domain.model.ExchangeRate;
import com.tutorial.redis.module04.domain.port.outbound.ExchangeRateCachePort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis adapter for exchange rate cache operations.
 * Supports the Cache-Aside (Lazy Loading) pattern.
 * Uses a 5-minute TTL for cached exchange rates.
 */
@Component
public class RedisExchangeRateCacheAdapter implements ExchangeRateCachePort {

    private static final String SERVICE = "cache";
    private static final String ENTITY = "exchange-rate";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisExchangeRateCacheAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(ExchangeRate exchangeRate) {
        String key = buildKey(exchangeRate.getCurrencyPair());
        redisTemplate.opsForValue().set(key, exchangeRate, DEFAULT_TTL);
    }

    @Override
    public Optional<ExchangeRate> findByPair(String pair) {
        String key = buildKey(pair);
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof ExchangeRate exchangeRate) {
            return Optional.of(exchangeRate);
        }
        return Optional.empty();
    }

    @Override
    public void evict(String pair) {
        redisTemplate.delete(buildKey(pair));
    }

    private String buildKey(String currencyPair) {
        return RedisKeyConvention.buildKey(SERVICE, ENTITY, currencyPair);
    }
}
