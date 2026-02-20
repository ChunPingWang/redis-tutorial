package com.tutorial.redis.module06.application.usecase;

import com.tutorial.redis.module06.domain.model.ExchangeRateSnapshot;
import com.tutorial.redis.module06.domain.port.inbound.QueryExchangeRateUseCase;
import com.tutorial.redis.module06.domain.port.outbound.ExchangeRateTimeSeriesPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service for recording and querying exchange rate time-series data.
 * Delegates all operations to the {@link ExchangeRateTimeSeriesPort} outbound port
 * which models the data using Redis Sorted Sets.
 */
@Service
public class QueryExchangeRateService implements QueryExchangeRateUseCase {

    private static final Logger log = LoggerFactory.getLogger(QueryExchangeRateService.class);

    private final ExchangeRateTimeSeriesPort exchangeRateTimeSeriesPort;

    public QueryExchangeRateService(ExchangeRateTimeSeriesPort exchangeRateTimeSeriesPort) {
        this.exchangeRateTimeSeriesPort = exchangeRateTimeSeriesPort;
    }

    @Override
    public void recordRate(ExchangeRateSnapshot snapshot) {
        log.debug("Recording rate for {} at timestamp {}", snapshot.getCurrencyPair(), snapshot.getTimestamp());
        exchangeRateTimeSeriesPort.addSnapshot(snapshot);
    }

    @Override
    public List<ExchangeRateSnapshot> queryRates(String currencyPair, long fromEpoch, long toEpoch) {
        log.debug("Querying rates for {} in range [{}, {}]", currencyPair, fromEpoch, toEpoch);
        return exchangeRateTimeSeriesPort.getSnapshots(currencyPair, fromEpoch, toEpoch);
    }

    @Override
    public Optional<ExchangeRateSnapshot> getLatestRate(String currencyPair) {
        log.debug("Retrieving latest rate for {}", currencyPair);
        return exchangeRateTimeSeriesPort.getLatestSnapshot(currencyPair);
    }
}
