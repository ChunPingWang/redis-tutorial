package com.tutorial.redis.module07.application.usecase;

import com.tutorial.redis.module07.domain.model.StreamMessage;
import com.tutorial.redis.module07.domain.port.outbound.StreamProducerPort;
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
@DisplayName("ManageStreamService 單元測試")
class ManageStreamServiceTest {

    @Mock
    private StreamProducerPort streamProducerPort;

    @InjectMocks
    private ManageStreamService service;

    @Test
    @DisplayName("addMessage_DelegatesToPort — 新增消息應委派給 StreamProducerPort.addToStream")
    void addMessage_DelegatesToPort() {
        // Arrange
        String streamKey = "my-stream";
        Map<String, String> fields = Map.of("action", "transfer", "amount", "250");
        when(streamProducerPort.addToStream(streamKey, fields)).thenReturn("1609459200000-0");

        // Act
        String messageId = service.addMessage(streamKey, fields);

        // Assert
        assertThat(messageId).isEqualTo("1609459200000-0");
        verify(streamProducerPort, times(1)).addToStream(streamKey, fields);
    }

    @Test
    @DisplayName("readMessages_DelegatesToPort — 讀取消息應委派給 StreamProducerPort.readMessages")
    void readMessages_DelegatesToPort() {
        // Arrange
        String streamKey = "my-stream";
        String fromId = "0-0";
        int count = 5;
        List<StreamMessage> expected = List.of(
                new StreamMessage("1609459200000-0", streamKey,
                        Map.of("key", "val1"), Instant.now()),
                new StreamMessage("1609459200001-0", streamKey,
                        Map.of("key", "val2"), Instant.now())
        );
        when(streamProducerPort.readMessages(streamKey, fromId, count)).thenReturn(expected);

        // Act
        List<StreamMessage> result = service.readMessages(streamKey, fromId, count);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPayload()).containsEntry("key", "val1");
        assertThat(result.get(1).getPayload()).containsEntry("key", "val2");
        verify(streamProducerPort, times(1)).readMessages(streamKey, fromId, count);
    }
}
