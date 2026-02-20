package com.tutorial.redis.module07.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module07.domain.model.StreamMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisStreamProducerAdapter 整合測試")
class RedisStreamProducerAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisStreamProducerAdapter adapter;

    @Test
    @DisplayName("addToStream_ReturnsMessageId — 新增消息到 Stream 後，應回傳非空的 Message ID")
    void addToStream_ReturnsMessageId() {
        // Arrange
        String streamKey = "test-stream:add";
        Map<String, String> payload = Map.of("action", "deposit", "amount", "100");

        // Act
        String messageId = adapter.addToStream(streamKey, payload);

        // Assert
        assertThat(messageId).isNotNull().isNotEmpty();
        assertThat(messageId).contains("-");
    }

    @Test
    @DisplayName("readMessages_ReturnsAddedMessages — 新增 3 筆消息後讀取，應回傳 3 筆且 payload 正確")
    void readMessages_ReturnsAddedMessages() {
        // Arrange
        String streamKey = "test-stream:read";
        adapter.addToStream(streamKey, Map.of("key", "value1"));
        adapter.addToStream(streamKey, Map.of("key", "value2"));
        adapter.addToStream(streamKey, Map.of("key", "value3"));

        // Act
        List<StreamMessage> messages = adapter.readMessages(streamKey, "0-0", 10);

        // Assert
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getPayload()).containsEntry("key", "value1");
        assertThat(messages.get(1).getPayload()).containsEntry("key", "value2");
        assertThat(messages.get(2).getPayload()).containsEntry("key", "value3");
        assertThat(messages).allSatisfy(msg -> {
            assertThat(msg.getStreamKey()).isEqualTo(streamKey);
            assertThat(msg.getMessageId()).isNotEmpty();
            assertThat(msg.getTimestamp()).isNotNull();
        });
    }

    @Test
    @DisplayName("rangeMessages_ReturnsMessagesInRange — 新增 3 筆消息後以範圍查詢，應回傳指定範圍內的全部消息")
    void rangeMessages_ReturnsMessagesInRange() {
        // Arrange
        String streamKey = "test-stream:range";
        String id1 = adapter.addToStream(streamKey, Map.of("seq", "1"));
        String id2 = adapter.addToStream(streamKey, Map.of("seq", "2"));
        String id3 = adapter.addToStream(streamKey, Map.of("seq", "3"));

        // Act
        List<StreamMessage> messages = adapter.rangeMessages(streamKey, id1, id3);

        // Assert
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getMessageId()).isEqualTo(id1);
        assertThat(messages.get(1).getMessageId()).isEqualTo(id2);
        assertThat(messages.get(2).getMessageId()).isEqualTo(id3);
    }

    @Test
    @DisplayName("trimStream_ReducesStreamLength — 新增 5 筆消息後裁剪至 2 筆，應僅保留 2 筆消息")
    void trimStream_ReducesStreamLength() {
        // Arrange
        String streamKey = "test-stream:trim";
        for (int i = 1; i <= 5; i++) {
            adapter.addToStream(streamKey, Map.of("seq", String.valueOf(i)));
        }

        // Act
        adapter.trimStream(streamKey, 2);

        // Assert
        List<StreamMessage> remaining = adapter.readMessages(streamKey, "0-0", 10);
        assertThat(remaining).hasSize(2);
    }
}
