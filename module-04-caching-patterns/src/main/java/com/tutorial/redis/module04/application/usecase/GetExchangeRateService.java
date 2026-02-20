package com.tutorial.redis.module04.application.usecase;

import com.tutorial.redis.module04.domain.model.ExchangeRate;
import com.tutorial.redis.module04.domain.port.inbound.GetExchangeRateUseCase;
import com.tutorial.redis.module04.domain.port.outbound.ExchangeRateCachePort;
import com.tutorial.redis.module04.domain.port.outbound.ExchangeRateRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Application service implementing the Cache-Aside (Lazy Loading) pattern
 * for exchange rate retrieval.
 *
 * <p>Flow: check cache -> on miss, query repository -> save to cache -> return.</p>
 */
@Service
public class GetExchangeRateService implements GetExchangeRateUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetExchangeRateService.class);

    private final ExchangeRateCachePort cachePort;
    private final ExchangeRateRepositoryPort repositoryPort;

    public GetExchangeRateService(ExchangeRateCachePort cachePort,
                                  ExchangeRateRepositoryPort repositoryPort) {
        this.cachePort = cachePort;
        this.repositoryPort = repositoryPort;
    }

    @Override
    public ExchangeRate getRate(String currencyPair) {
        // Step 1: Check cache
        Optional<ExchangeRate> cached = cachePort.findByPair(currencyPair);
        if (cached.isPresent()) {
            log.debug("Cache HIT for exchange rate: {}", currencyPair);
            return cached.get();
        }

        // Step 2: Cache miss — query repository
        log.debug("Cache MISS for exchange rate: {}, querying repository", currencyPair);
        ExchangeRate rate = repositoryPort.findByPair(currencyPair)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported currency pair: " + currencyPair));

        // Step 3: Save to cache
        cachePort.save(rate);
        log.debug("Cached exchange rate: {}", currencyPair);

        return rate;
    }
}
