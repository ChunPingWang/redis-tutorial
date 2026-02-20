package com.tutorial.redis.module07.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module07.domain.model.AccountEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisEventStoreAdapter 整合測試")
class RedisEventStoreAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisEventStoreAdapter adapter;

    private static final String ACCOUNT_ID = "acc-test-001";
    private static final String STREAM_KEY = "event:account:" + ACCOUNT_ID;

    @Test
    @DisplayName("appendAndReadAll_ReturnsEvents — 依序新增 3 筆事件後讀取全部，應回傳正確的事件類型與金額")
    void appendAndReadAll_ReturnsEvents() {
        // Arrange
        Instant now = Instant.now();
        AccountEvent openEvent = new AccountEvent(
                "tmp-1", ACCOUNT_ID, "ACCOUNT_OPENED", null, now, Map.of("source", "online"));
        AccountEvent depositEvent = new AccountEvent(
                "tmp-2", ACCOUNT_ID, "MONEY_DEPOSITED", 500.0, now.plusSeconds(1), Map.of());
        AccountEvent withdrawEvent = new AccountEvent(
                "tmp-3", ACCOUNT_ID, "MONEY_WITHDRAWN", 200.0, now.plusSeconds(2), Map.of());

        // Act
        adapter.appendEvent(STREAM_KEY, openEvent);
        adapter.appendEvent(STREAM_KEY, depositEvent);
        adapter.appendEvent(STREAM_KEY, withdrawEvent);

        List<AccountEvent> events = adapter.readAllEvents(STREAM_KEY);

        // Assert
        assertThat(events).hasSize(3);

        assertThat(events.get(0).getEventType()).isEqualTo("ACCOUNT_OPENED");
        assertThat(events.get(0).getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(events.get(0).getAmount()).isNull();

        assertThat(events.get(1).getEventType()).isEqualTo("MONEY_DEPOSITED");
        assertThat(events.get(1).getAmount()).isEqualTo(500.0);

        assertThat(events.get(2).getEventType()).isEqualTo("MONEY_WITHDRAWN");
        assertThat(events.get(2).getAmount()).isEqualTo(200.0);

        // Verify event IDs are Redis-assigned (not the tmp ones)
        assertThat(events).allSatisfy(event -> {
            assertThat(event.getEventId()).contains("-");
            assertThat(event.getEventId()).doesNotStartWith("tmp");
        });
    }

    @Test
    @DisplayName("readEventsFrom_ReturnsSubsequentEvents — 新增 3 筆事件後從第 2 筆 ID 開始讀取，應回傳 2 筆事件")
    void readEventsFrom_ReturnsSubsequentEvents() {
        // Arrange
        Instant now = Instant.now();
        AccountEvent event1 = new AccountEvent(
                "tmp-1", ACCOUNT_ID, "ACCOUNT_OPENED", null, now, Map.of());
        AccountEvent event2 = new AccountEvent(
                "tmp-2", ACCOUNT_ID, "MONEY_DEPOSITED", 300.0, now.plusSeconds(1), Map.of());
        AccountEvent event3 = new AccountEvent(
                "tmp-3", ACCOUNT_ID, "MONEY_DEPOSITED", 200.0, now.plusSeconds(2), Map.of());

        adapter.appendEvent(STREAM_KEY, event1);
        String secondEventId = adapter.appendEvent(STREAM_KEY, event2);
        adapter.appendEvent(STREAM_KEY, event3);

        // Act — read from the second event ID (inclusive)
        List<AccountEvent> events = adapter.readEventsFrom(STREAM_KEY, secondEventId);

        // Assert
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo("MONEY_DEPOSITED");
        assertThat(events.get(0).getAmount()).isEqualTo(300.0);
        assertThat(events.get(1).getEventType()).isEqualTo("MONEY_DEPOSITED");
        assertThat(events.get(1).getAmount()).isEqualTo(200.0);
    }
}
