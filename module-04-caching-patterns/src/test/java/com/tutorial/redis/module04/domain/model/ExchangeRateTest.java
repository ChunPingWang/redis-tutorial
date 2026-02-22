package com.tutorial.redis.module04.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 匯率領域模型測試。
 * 驗證 ExchangeRate 物件的建構與參數驗證邏輯。
 * 確保不合法的貨幣對或匯率值在建構時即被拒絕，維護領域模型的不變量。
 * 屬於 Domain 層（領域模型）。
 */
@DisplayName("ExchangeRate 領域模型測試")
class ExchangeRateTest {

    // 驗證使用有效參數可正確建立 ExchangeRate 物件
    @Test
    @DisplayName("constructor_Valid — 有效參數建立匯率物件成功")
    void constructor_Valid() {
        Instant now = Instant.now();

        ExchangeRate rate = new ExchangeRate("USD/TWD", 31.5, now);

        assertThat(rate.getCurrencyPair()).isEqualTo("USD/TWD");
        assertThat(rate.getRate()).isEqualTo(31.5);
        assertThat(rate.getTimestamp()).isEqualTo(now);
    }

    // 驗證貨幣對為 null 時拋出 NullPointerException
    @Test
    @DisplayName("constructor_NullPair_ThrowsNPE — 貨幣對為 null 時拋出 NullPointerException")
    void constructor_NullPair_ThrowsNPE() {
        assertThatThrownBy(() -> new ExchangeRate(null, 31.5, Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currencyPair must not be null");
    }

    // 驗證匯率為負值時拋出 IllegalArgumentException
    @Test
    @DisplayName("constructor_NegativeRate_ThrowsIAE — 負匯率時拋出 IllegalArgumentException")
    void constructor_NegativeRate_ThrowsIAE() {
        assertThatThrownBy(() -> new ExchangeRate("USD/TWD", -1.0, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rate must be positive");
    }
}
