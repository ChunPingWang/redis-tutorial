package com.tutorial.redis.module07.application.usecase;

import com.tutorial.redis.module07.domain.model.AccountEvent;
import com.tutorial.redis.module07.domain.model.AccountState;
import com.tutorial.redis.module07.domain.port.outbound.EventStorePort;
import com.tutorial.redis.module07.domain.service.EventReplayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 驗證 EventSourcingService 的事件溯源應用服務邏輯。
 * 測試事件的寫入委派（透過 XADD 寫入 Stream）與狀態重建（讀取全部事件後重播），
 * 展示 Event Sourcing 模式在 Redis Streams 上的實作。
 * 所屬層級：Application 層（Use Case 單元測試，使用 Mock 隔離）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventSourcingService 單元測試")
class EventSourcingServiceTest {

    @Mock
    private EventStorePort eventStorePort;

    @Mock
    private EventReplayService eventReplayService;

    @InjectMocks
    private EventSourcingService service;

    // 驗證新增帳戶事件時，正確組合 streamKey 並委派給 EventStorePort 寫入
    @Test
    @DisplayName("appendAccountEvent_DelegatesToPort — 新增事件應委派給 EventStorePort，且 streamKey 格式為 event:account:{accountId}")
    void appendAccountEvent_DelegatesToPort() {
        // Arrange
        String accountId = "acc-001";
        AccountEvent event = new AccountEvent(
                "evt-1", accountId, "MONEY_DEPOSITED", 100.0, Instant.now(), Map.of());
        String expectedStreamKey = "event:account:" + accountId;
        when(eventStorePort.appendEvent(expectedStreamKey, event)).thenReturn("1234567890-0");

        // Act
        String eventId = service.appendAccountEvent(event);

        // Assert
        assertThat(eventId).isEqualTo("1234567890-0");
        verify(eventStorePort, times(1)).appendEvent(expectedStreamKey, event);
    }

    // 驗證重播事件流程：先從 EventStorePort 讀取全部事件，再交由 EventReplayService 重建最終狀態
    @Test
    @DisplayName("replayEvents_ReadsThenReplays — 重播事件應先讀取所有事件，再委派給 EventReplayService 重建狀態")
    void replayEvents_ReadsThenReplays() {
        // Arrange
        String accountId = "acc-002";
        String streamKey = "event:account:" + accountId;
        Instant now = Instant.now();

        List<AccountEvent> events = List.of(
                new AccountEvent("e1", accountId, "ACCOUNT_OPENED", null, now, Map.of()),
                new AccountEvent("e2", accountId, "MONEY_DEPOSITED", 500.0, now.plusSeconds(1), Map.of()),
                new AccountEvent("e3", accountId, "MONEY_WITHDRAWN", 100.0, now.plusSeconds(2), Map.of())
        );

        AccountState expectedState = new AccountState(accountId, 400.0, "ACTIVE", 3);

        when(eventStorePort.readAllEvents(streamKey)).thenReturn(events);
        when(eventReplayService.replay(events)).thenReturn(expectedState);

        // Act
        AccountState result = service.replayEvents(accountId);

        // Assert
        assertThat(result.getAccountId()).isEqualTo(accountId);
        assertThat(result.getBalance()).isEqualTo(400.0);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getEventCount()).isEqualTo(3);

        verify(eventStorePort, times(1)).readAllEvents(streamKey);
        verify(eventReplayService, times(1)).replay(events);
    }
}
