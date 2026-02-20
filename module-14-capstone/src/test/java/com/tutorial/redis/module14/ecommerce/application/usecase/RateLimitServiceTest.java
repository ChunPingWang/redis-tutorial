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

@DisplayName("RateLimitService 單元測試")
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RateLimiterPort rateLimiterPort;

    @InjectMocks
    private RateLimitService rateLimitService;

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
