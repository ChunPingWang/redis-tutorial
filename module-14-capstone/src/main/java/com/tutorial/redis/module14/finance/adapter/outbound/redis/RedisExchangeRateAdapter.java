package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.module14.finance.domain.port.outbound.ExchangeRatePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Redis adapter for exchange rate time-series operations.
 *
 * <p>Implements {@link ExchangeRatePort} using Redis TimeSeries module
 * commands ({@code TS.CREATE}, {@code TS.ADD}, {@code TS.GET}) via Lua
 * scripts for recording and retrieving currency exchange rates.</p>
 */
@Component
public class RedisExchangeRateAdapter implements ExchangeRatePort {

    private static final Logger log = LoggerFactory.getLogger(RedisExchangeRateAdapter.class);

    private static final String RATE_KEY_PREFIX = "finance:rate:";

    /**
     * Lua script for TS.ADD: adds a data point to a time series.
     * Creates the time series with TS.CREATE if it does not exist.
     */
    private static final DefaultRedisScript<String> TS_ADD_SCRIPT = new DefaultRedisScript<>(
            "local ok, err = pcall(redis.call, 'TS.ADD', KEYS[1], ARGV[1], ARGV[2])\n"
                    + "if not ok then\n"
                    + "  redis.call('TS.CREATE', KEYS[1])\n"
                    + "  redis.call('TS.ADD', KEYS[1], ARGV[1], ARGV[2])\n"
                    + "end\n"
                    + "return 'OK'",
            String.class);

    /**
     * Lua script for TS.GET: retrieves the latest data point value.
     * In Redis 8 with RESP3, double values are returned as Lua tables
     * with an {@code ok} field (e.g., {@code {ok = 0.87}}), so we must
     * check the type and extract accordingly.
     */
    private static final DefaultRedisScript<String> TS_GET_SCRIPT = new DefaultRedisScript<>(
            "local ok, r = pcall(redis.call, 'TS.GET', KEYS[1])\n"
                    + "if not ok or not r then return nil end\n"
                    + "local val = r[2]\n"
                    + "if type(val) == 'table' then\n"
                    + "  if val.ok ~= nil then return tostring(val.ok) end\n"
                    + "  return tostring(val[1])\n"
                    + "end\n"
                    + "return tostring(val)",
            String.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisExchangeRateAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void recordRate(String pair, double rate, long timestamp) {
        String key = RATE_KEY_PREFIX + pair;
        List<String> keys = Collections.singletonList(key);
        try {
            stringRedisTemplate.execute(TS_ADD_SCRIPT, keys,
                    String.valueOf(timestamp), String.valueOf(rate));
            log.debug("Recorded rate for {}: {} at {}", pair, rate, timestamp);
        } catch (Exception e) {
            log.warn("TS.ADD failed for {}: {}", pair, e.getMessage());
        }
    }

    @Override
    public Double getLatestRate(String pair) {
        String key = RATE_KEY_PREFIX + pair;
        List<String> keys = Collections.singletonList(key);
        try {
            String result = stringRedisTemplate.execute(TS_GET_SCRIPT, keys);
            if (result == null || result.isEmpty()) {
                log.debug("No time series data for {}", pair);
                return null;
            }
            return Double.parseDouble(result);
        } catch (Exception e) {
            log.warn("TS.GET failed for {}: {}", pair, e.getMessage());
            return null;
        }
    }
}
