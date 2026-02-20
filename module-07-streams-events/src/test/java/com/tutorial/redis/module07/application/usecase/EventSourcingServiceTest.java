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

@ExtendWith(MockitoExtension.class)
@DisplayName("EventSourcingService 單元測試")
class EventSourcingServiceTest {

    @Mock
    private EventStorePort eventStorePort;

    @Mock
    private EventReplayService eventReplayService;

    @InjectMocks
    private EventSourcingService service;

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
