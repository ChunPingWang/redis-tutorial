package com.tutorial.redis.module04.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheTtlService 領域服務測試")
class CacheTtlServiceTest {

    private final CacheTtlService service = new CacheTtlService();

    @Test
    @DisplayName("randomizeTtl_ReturnsValueInRange — 隨機 TTL 落在 [base, base*(1+spread)) 範圍內")
    void randomizeTtl_ReturnsValueInRange() {
        long baseTtl = 60_000L;
        double spread = 0.3;
        long maxTtl = baseTtl + (long) (baseTtl * spread); // 78_000

        for (int i = 0; i < 100; i++) {
            long result = service.randomizeTtl(baseTtl, spread);

            assertThat(result)
                    .as("Iteration %d: TTL should be in range [%d, %d)", i, baseTtl, maxTtl)
                    .isGreaterThanOrEqualTo(baseTtl)
                    .isLessThan(maxTtl);
        }
    }

    @Test
    @DisplayName("shouldRefresh_WhenBelow20Percent_ReturnsTrue — 剩餘 TTL 低於 20% 時應刷新")
    void shouldRefresh_WhenBelow20Percent_ReturnsTrue() {
        // 10s remaining out of 60s = 16.7% < 20%
        boolean result = service.shouldRefresh(10_000, 60_000);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("shouldRefresh_WhenAbove20Percent_ReturnsFalse — 剩餘 TTL 高於 20% 時不應刷新")
    void shouldRefresh_WhenAbove20Percent_ReturnsFalse() {
        // 30s remaining out of 60s = 50% > 20%
        boolean result = service.shouldRefresh(30_000, 60_000);

        assertThat(result).isFalse();
    }
}
