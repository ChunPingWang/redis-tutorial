package com.tutorial.redis.module07.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module07.domain.model.PendingMessage;
import com.tutorial.redis.module07.domain.model.StreamMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("RedisConsumerGroupAdapter 整合測試")
class RedisConsumerGroupAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisConsumerGroupAdapter consumerGroupAdapter;

    @Autowired
    private RedisStreamProducerAdapter producerAdapter;

    @Test
    @DisplayName("createGroup_AndReadFromGroup_ReturnsMessages — 建立 Consumer Group 後讀取，應回傳所有已新增的消息")
    void createGroup_AndReadFromGroup_ReturnsMessages() {
        // Arrange
        String streamKey = "test-stream:cg-read";
        producerAdapter.addToStream(streamKey, Map.of("event", "login"));
        producerAdapter.addToStream(streamKey, Map.of("event", "purchase"));
        producerAdapter.addToStream(streamKey, Map.of("event", "logout"));

        consumerGroupAdapter.createGroup(streamKey, "my-group");

        // Act
        List<StreamMessage> messages = consumerGroupAdapter.readFromGroup(
                streamKey, "my-group", "consumer-1", 10);

        // Assert
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getPayload()).containsEntry("event", "login");
        assertThat(messages.get(1).getPayload()).containsEntry("event", "purchase");
        assertThat(messages.get(2).getPayload()).containsEntry("event", "logout");
    }

    @Test
    @DisplayName("acknowledge_RemovesFromPending — 讀取消息後確認，pending 列表應為空")
    void acknowledge_RemovesFromPending() {
        // Arrange
        String streamKey = "test-stream:cg-ack";
        producerAdapter.addToStream(streamKey, Map.of("data", "test-value"));
        consumerGroupAdapter.createGroup(streamKey, "ack-group");

        // Read to create pending entry
        List<StreamMessage> messages = consumerGroupAdapter.readFromGroup(
                streamKey, "ack-group", "consumer-1", 10);
        assertThat(messages).hasSize(1);

        // Verify pending exists before ack
        List<PendingMessage> pendingBefore = consumerGroupAdapter.getPendingMessages(
                streamKey, "ack-group", 10);
        assertThat(pendingBefore).hasSize(1);

        // Act — acknowledge the message
        String messageId = messages.getFirst().getMessageId();
        consumerGroupAdapter.acknowledge(streamKey, "ack-group", messageId);

        // Assert — pending should be empty after ack
        List<PendingMessage> pendingAfter = consumerGroupAdapter.getPendingMessages(
                streamKey, "ack-group", 10);
        assertThat(pendingAfter).isEmpty();
    }

    @Test
    @DisplayName("createGroup_WhenAlreadyExists_DoesNotThrow — 重複建立相同 Consumer Group 不應拋出例外")
    void createGroup_WhenAlreadyExists_DoesNotThrow() {
        // Arrange
        String streamKey = "test-stream:cg-dup";
        producerAdapter.addToStream(streamKey, Map.of("init", "true"));
        consumerGroupAdapter.createGroup(streamKey, "duplicate-group");

        // Act & Assert — creating the same group again should not throw
        assertThatCode(() -> consumerGroupAdapter.createGroup(streamKey, "duplicate-group"))
                .doesNotThrowAnyException();
    }
}
