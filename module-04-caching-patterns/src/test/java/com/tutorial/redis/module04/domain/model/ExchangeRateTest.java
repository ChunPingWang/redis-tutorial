package com.tutorial.redis.module04.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExchangeRate 領域模型測試")
class ExchangeRateTest {

    @Test
    @DisplayName("constructor_Valid — 有效參數建立匯率物件成功")
    void constructor_Valid() {
        Instant now = Instant.now();

        ExchangeRate rate = new ExchangeRate("USD/TWD", 31.5, now);

        assertThat(rate.getCurrencyPair()).isEqualTo("USD/TWD");
        assertThat(rate.getRate()).isEqualTo(31.5);
        assertThat(rate.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("constructor_NullPair_ThrowsNPE — 貨幣對為 null 時拋出 NullPointerException")
    void constructor_NullPair_ThrowsNPE() {
        assertThatThrownBy(() -> new ExchangeRate(null, 31.5, Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currencyPair must not be null");
    }

    @Test
    @DisplayName("constructor_NegativeRate_ThrowsIAE — 負匯率時拋出 IllegalArgumentException")
    void constructor_NegativeRate_ThrowsIAE() {
        assertThatThrownBy(() -> new ExchangeRate("USD/TWD", -1.0, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rate must be positive");
    }
}
