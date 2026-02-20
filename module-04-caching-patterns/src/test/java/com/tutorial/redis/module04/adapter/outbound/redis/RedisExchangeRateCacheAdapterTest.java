package com.tutorial.redis.module04.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module04.domain.model.ExchangeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisExchangeRateCacheAdapter 整合測試")
class RedisExchangeRateCacheAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisExchangeRateCacheAdapter adapter;

    private ExchangeRate createRate(String pair, double rate) {
        return new ExchangeRate(pair, rate, Instant.now());
    }

    @Test
    @DisplayName("save_AndFind_ReturnsRate — 儲存匯率後可查詢回來")
    void save_AndFind_ReturnsRate() {
        ExchangeRate rate = createRate("USD/TWD", 31.5);

        adapter.save(rate);
        Optional<ExchangeRate> found = adapter.findByPair("USD/TWD");

        assertThat(found).isPresent();
        assertThat(found.get().getCurrencyPair()).isEqualTo("USD/TWD");
        assertThat(found.get().getRate()).isEqualTo(31.5);
    }

    @Test
    @DisplayName("findByPair_WhenNotExists_ReturnsEmpty — 查詢不存在的匯率回傳空")
    void findByPair_WhenNotExists_ReturnsEmpty() {
        Optional<ExchangeRate> found = adapter.findByPair("NON/EXIST");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("evict_RemovesFromCache — 驅逐後快取不再存在")
    void evict_RemovesFromCache() {
        ExchangeRate rate = createRate("EUR/TWD", 34.2);
        adapter.save(rate);
        assertThat(adapter.findByPair("EUR/TWD")).isPresent();

        adapter.evict("EUR/TWD");

        assertThat(adapter.findByPair("EUR/TWD")).isEmpty();
    }

    @Test
    @DisplayName("save_SetsExpiration — 儲存時設定 TTL 過期時間")
    void save_SetsExpiration() {
        ExchangeRate rate = createRate("GBP/TWD", 40.1);

        adapter.save(rate);

        Long ttl = stringRedisTemplate.getExpire("cache:exchange-rate:GBP/TWD", TimeUnit.SECONDS);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(0);
        // Default TTL is 5 minutes = 300 seconds
        assertThat(ttl).isLessThanOrEqualTo(300);
    }
}
