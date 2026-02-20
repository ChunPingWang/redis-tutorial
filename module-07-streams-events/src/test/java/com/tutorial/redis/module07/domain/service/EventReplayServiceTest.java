package com.tutorial.redis.module07.domain.service;

import com.tutorial.redis.module07.domain.model.AccountEvent;
import com.tutorial.redis.module07.domain.model.AccountState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventReplayService 領域服務測試")
class EventReplayServiceTest {

    private final EventReplayService service = new EventReplayService();

    @Test
    @DisplayName("replay_MultipleEvents_ReturnsCorrectFinalState — 重播開戶、存款 500、提款 200、存款 100，最終餘額應為 400")
    void replay_MultipleEvents_ReturnsCorrectFinalState() {
        // Arrange
        String accountId = "acc-replay";
        Instant now = Instant.now();

        List<AccountEvent> events = List.of(
                new AccountEvent("e1", accountId, "ACCOUNT_OPENED", null, now, Map.of()),
                new AccountEvent("e2", accountId, "MONEY_DEPOSITED", 500.0, now.plusSeconds(1), Map.of()),
                new AccountEvent("e3", accountId, "MONEY_WITHDRAWN", 200.0, now.plusSeconds(2), Map.of()),
                new AccountEvent("e4", accountId, "MONEY_DEPOSITED", 100.0, now.plusSeconds(3), Map.of())
        );

        // Act
        AccountState state = service.replay(events);

        // Assert
        assertThat(state.getAccountId()).isEqualTo(accountId);
        assertThat(state.getBalance()).isEqualTo(400.0);
        assertThat(state.getStatus()).isEqualTo("ACTIVE");
        assertThat(state.getEventCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("replay_EmptyEvents_ThrowsIAE — 空事件列表應拋出 IllegalArgumentException")
    void replay_EmptyEvents_ThrowsIAE() {
        // Arrange
        List<AccountEvent> emptyEvents = Collections.emptyList();

        // Act & Assert
        assertThatThrownBy(() -> service.replay(emptyEvents))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("events must not be empty");
    }
}
