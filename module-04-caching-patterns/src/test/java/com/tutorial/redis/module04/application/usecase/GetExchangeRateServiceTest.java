package com.tutorial.redis.module04.application.usecase;

import com.tutorial.redis.module04.domain.model.ExchangeRate;
import com.tutorial.redis.module04.domain.port.outbound.ExchangeRateCachePort;
import com.tutorial.redis.module04.domain.port.outbound.ExchangeRateRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 取得匯率服務單元測試。
 * 驗證 Cache-Aside（旁路快取）模式的應用層邏輯：先查快取，未命中再查資料庫並回寫快取。
 * 使用 Mockito 模擬快取端口與資料庫端口，確保快取命中/未命中/資料不存在三種情境。
 * 屬於 Application 層（應用服務 / Use Case）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetExchangeRateService 單元測試")
class GetExchangeRateServiceTest {

    @Mock
    private ExchangeRateCachePort cachePort;

    @Mock
    private ExchangeRateRepositoryPort repositoryPort;

    @InjectMocks
    private GetExchangeRateService service;

    private ExchangeRate createRate(String pair, double rate) {
        return new ExchangeRate(pair, rate, Instant.now());
    }

    // 驗證快取命中時直接回傳快取結果，不查詢資料庫（Cache-Aside 核心路徑）
    @Test
    @DisplayName("getRate_WhenCacheHit_ReturnsCachedRate — 快取命中時直接回傳且不查詢資料庫")
    void getRate_WhenCacheHit_ReturnsCachedRate() {
        ExchangeRate cached = createRate("USD/TWD", 31.5);
        when(cachePort.findByPair("USD/TWD")).thenReturn(Optional.of(cached));

        ExchangeRate result = service.getRate("USD/TWD");

        assertThat(result.getCurrencyPair()).isEqualTo("USD/TWD");
        assertThat(result.getRate()).isEqualTo(31.5);
        verify(cachePort).findByPair("USD/TWD");
        verify(repositoryPort, never()).findByPair(anyString());
    }

    // 驗證快取未命中時查詢資料庫，取得結果後回寫快取（Cache-Aside 回填路徑）
    @Test
    @DisplayName("getRate_WhenCacheMiss_QueriesRepoAndCaches — 快取未命中時查詢資料庫並寫入快取")
    void getRate_WhenCacheMiss_QueriesRepoAndCaches() {
        ExchangeRate fromRepo = createRate("EUR/TWD", 34.2);
        when(cachePort.findByPair("EUR/TWD")).thenReturn(Optional.empty());
        when(repositoryPort.findByPair("EUR/TWD")).thenReturn(Optional.of(fromRepo));

        ExchangeRate result = service.getRate("EUR/TWD");

        assertThat(result.getCurrencyPair()).isEqualTo("EUR/TWD");
        assertThat(result.getRate()).isEqualTo(34.2);
        verify(cachePort).findByPair("EUR/TWD");
        verify(repositoryPort).findByPair("EUR/TWD");
        verify(cachePort).save(fromRepo);
    }

    // 驗證快取與資料庫皆找不到時拋出 IllegalArgumentException
    @Test
    @DisplayName("getRate_WhenNotFound_ThrowsException — 資料庫也找不到時拋出例外")
    void getRate_WhenNotFound_ThrowsException() {
        when(cachePort.findByPair("XYZ/ABC")).thenReturn(Optional.empty());
        when(repositoryPort.findByPair("XYZ/ABC")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRate("XYZ/ABC"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported currency pair");

        verify(cachePort).findByPair("XYZ/ABC");
        verify(repositoryPort).findByPair("XYZ/ABC");
        verify(cachePort, never()).save(any());
    }
}
