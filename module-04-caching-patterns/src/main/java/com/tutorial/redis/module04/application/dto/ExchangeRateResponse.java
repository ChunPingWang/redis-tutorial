package com.tutorial.redis.module04.application.dto;

import java.time.Instant;

/**
 * Response DTO for exchange rate queries.
 *
 * @param currencyPair the currency pair (e.g. "USD/TWD")
 * @param rate         the exchange rate value
 * @param timestamp    when the rate was recorded
 * @param source       where the rate was retrieved from: "CACHE" or "REPOSITORY"
 */
public record ExchangeRateResponse(
        String currencyPair,
        double rate,
        Instant timestamp,
        String source
) {
}
