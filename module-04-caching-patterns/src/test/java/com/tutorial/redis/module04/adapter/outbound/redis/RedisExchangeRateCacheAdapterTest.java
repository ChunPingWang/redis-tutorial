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

/**
 * Redis 匯率快取適配器整合測試。
 * 驗證 Cache-Aside 模式下匯率資料的快取存取、驅逐與 TTL 過期機制。
 * 測試快取的基本 CRUD 操作與自動過期策略。
 * 屬於 Adapter 層（外部基礎設施適配器）。
 */
@DisplayName("RedisExchangeRateCacheAdapter 整合測試")
class RedisExchangeRateCacheAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisExchangeRateCacheAdapter adapter;

    private ExchangeRate createRate(String pair, double rate) {
        return new ExchangeRate(pair, rate, Instant.now());
    }

    // 驗證匯率儲存至 Redis 快取後，可透過貨幣對正確查詢回來
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

    // 驗證查詢不存在的貨幣對時回傳 Optional.empty()
    @Test
    @DisplayName("findByPair_WhenNotExists_ReturnsEmpty — 查詢不存在的匯率回傳空")
    void findByPair_WhenNotExists_ReturnsEmpty() {
        Optional<ExchangeRate> found = adapter.findByPair("NON/EXIST");

        assertThat(found).isEmpty();
    }

    // 驗證快取驅逐（evict）操作能正確移除指定的匯率快取
    @Test
    @DisplayName("evict_RemovesFromCache — 驅逐後快取不再存在")
    void evict_RemovesFromCache() {
        ExchangeRate rate = createRate("EUR/TWD", 34.2);
        adapter.save(rate);
        assertThat(adapter.findByPair("EUR/TWD")).isPresent();

        adapter.evict("EUR/TWD");

        assertThat(adapter.findByPair("EUR/TWD")).isEmpty();
    }

    // 驗證儲存匯率時會自動設定 TTL，確保快取在預設 5 分鐘後過期
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
