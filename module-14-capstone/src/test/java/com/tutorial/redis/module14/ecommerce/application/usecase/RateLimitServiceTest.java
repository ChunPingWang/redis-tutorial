package com.tutorial.redis.module14.ecommerce.application.usecase;

import com.tutorial.redis.module14.ecommerce.domain.model.RateLimitResult;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.RateLimiterPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RateLimitService 應用層單元測試類別。
 * 驗證速率限制服務正確組裝 Key 前綴並委派給 RateLimiterPort。
 * 展示 Redis 計數器搭配 TTL 實現 API 限流的應用層邏輯。
 * 所屬：電商子系統 — application 層
 */
@DisplayName("RateLimitService 單元測試")
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RateLimiterPort rateLimiterPort;

    @InjectMocks
    private RateLimitService rateLimitService;

    // 驗證檢查速率限制時，正確組裝 Key 並委派給 RateLimiterPort
    @Test
    @DisplayName("checkRateLimit_DelegatesToPort — 檢查速率限制應委派給 RateLimiterPort")
    void checkRateLimit_DelegatesToPort() {
        // Arrange
        RateLimitResult expected = new RateLimitResult(true, 9, 0);
        when(rateLimiterPort.tryAcquire("ecommerce:ratelimit:client-1", 10, 60))
                .thenReturn(expected);

        // Act
        RateLimitResult result = rateLimitService.checkRateLimit("client-1", 10, 60);

        // Assert
        assertThat(result).isSameAs(expected);
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemainingTokens()).isEqualTo(9);
        verify(rateLimiterPort).tryAcquire("ecommerce:ratelimit:client-1", 10, 60);
    }
}
