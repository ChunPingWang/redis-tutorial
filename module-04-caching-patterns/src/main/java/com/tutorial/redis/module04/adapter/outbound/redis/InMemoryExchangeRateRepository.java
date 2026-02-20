package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.module04.domain.model.ExchangeRate;
import com.tutorial.redis.module04.domain.port.outbound.ExchangeRateRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory repository simulating an external data source or database
 * for exchange rates. Pre-populated with sample currency pairs.
 */
@Component
public class InMemoryExchangeRateRepository implements ExchangeRateRepositoryPort {

    private final ConcurrentMap<String, ExchangeRate> store = new ConcurrentHashMap<>();

    public InMemoryExchangeRateRepository() {
        Instant now = Instant.now();
        store.put("USD/TWD", new ExchangeRate("USD/TWD", 31.5, now));
        store.put("EUR/TWD", new ExchangeRate("EUR/TWD", 34.2, now));
        store.put("GBP/TWD", new ExchangeRate("GBP/TWD", 40.1, now));
        store.put("JPY/TWD", new ExchangeRate("JPY/TWD", 0.21, now));
    }

    @Override
    public Optional<ExchangeRate> findByPair(String pair) {
        return Optional.ofNullable(store.get(pair));
    }
}
