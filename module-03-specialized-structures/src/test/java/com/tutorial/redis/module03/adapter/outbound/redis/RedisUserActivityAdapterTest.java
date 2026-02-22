package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisModuleIntegrationTest;
import com.tutorial.redis.module03.domain.port.outbound.UserActivityPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis Bitmap 配接器整合測試
 * 驗證 UserActivityPort 透過 Redis Bitmap（SETBIT/GETBIT/BITCOUNT）命令的實作正確性
 * 涵蓋記錄活躍日、查詢單日活躍狀態與統計活躍天數，屬於 Adapter 層（外部輸出端）
 */
@DisplayName("RedisUserActivityAdapter 整合測試")
class RedisUserActivityAdapterTest extends AbstractRedisModuleIntegrationTest {

    @Autowired
    private UserActivityPort userActivityPort;

    private static final String USER_ID = "USER-001";
    private static final String YEAR_MONTH = "202602";

    // 驗證 SETBIT 記錄某日活躍後，GETBIT 查詢該日回傳 true
    @Test
    @DisplayName("recordActivity_AndIsActive_ReturnsTrue — 記錄活躍日後查詢回傳 true")
    void recordActivity_AndIsActive_ReturnsTrue() {
        userActivityPort.recordActivity(USER_ID, YEAR_MONTH, 1);

        boolean active = userActivityPort.isActive(USER_ID, YEAR_MONTH, 1);

        assertThat(active).isTrue();
    }

    // 驗證未記錄過的日期，GETBIT 查詢回傳 false
    @Test
    @DisplayName("isActive_WhenNotRecorded_ReturnsFalse — 未記錄的日期回傳 false")
    void isActive_WhenNotRecorded_ReturnsFalse() {
        boolean active = userActivityPort.isActive(USER_ID, YEAR_MONTH, 15);

        assertThat(active).isFalse();
    }

    // 驗證記錄 4 個活躍日後，BITCOUNT 計數回傳 4
    @Test
    @DisplayName("countActiveDays_WhenMultipleDays_ReturnsCount — 記錄多日後計數正確")
    void countActiveDays_WhenMultipleDays_ReturnsCount() {
        userActivityPort.recordActivity(USER_ID, YEAR_MONTH, 1);
        userActivityPort.recordActivity(USER_ID, YEAR_MONTH, 3);
        userActivityPort.recordActivity(USER_ID, YEAR_MONTH, 5);
        userActivityPort.recordActivity(USER_ID, YEAR_MONTH, 7);

        long count = userActivityPort.countActiveDays(USER_ID, YEAR_MONTH);

        assertThat(count).isEqualTo(4);
    }

    // 驗證連續記錄第 1~10 天後，每一天查詢皆為活躍
    @Test
    @DisplayName("recordActivity_MultipleDays_AllActive — 記錄連續 10 天全部為活躍")
    void recordActivity_MultipleDays_AllActive() {
        for (int day = 1; day <= 10; day++) {
            userActivityPort.recordActivity(USER_ID, YEAR_MONTH, day);
        }

        for (int day = 1; day <= 10; day++) {
            assertThat(userActivityPort.isActive(USER_ID, YEAR_MONTH, day))
                    .as("Day %d should be active", day)
                    .isTrue();
        }
    }

    // 驗證無任何活躍紀錄時，BITCOUNT 回傳 0
    @Test
    @DisplayName("countActiveDays_WhenNoDays_ReturnsZero — 無記錄時計數為零")
    void countActiveDays_WhenNoDays_ReturnsZero() {
        long count = userActivityPort.countActiveDays(USER_ID, YEAR_MONTH);

        assertThat(count).isZero();
    }
}
