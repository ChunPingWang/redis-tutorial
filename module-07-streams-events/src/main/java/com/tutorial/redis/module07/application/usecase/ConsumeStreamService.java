package com.tutorial.redis.module07.application.usecase;

import com.tutorial.redis.module07.domain.model.PendingMessage;
import com.tutorial.redis.module07.domain.model.StreamMessage;
import com.tutorial.redis.module07.domain.port.inbound.ConsumeStreamUseCase;
import com.tutorial.redis.module07.domain.port.outbound.ConsumerGroupPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service for Redis Stream consumer group operations.
 *
 * <p>Implements the {@link ConsumeStreamUseCase} inbound port by delegating
 * to the {@link ConsumerGroupPort} outbound port. Provides consumer group
 * creation, message consumption, acknowledgment, and pending message inspection.</p>
 */
@Service
public class ConsumeStreamService implements ConsumeStreamUseCase {

    private final ConsumerGroupPort consumerGroupPort;

    public ConsumeStreamService(ConsumerGroupPort consumerGroupPort) {
        this.consumerGroupPort = consumerGroupPort;
    }

    /**
     * Creates a consumer group for the specified stream.
     *
     * @param streamKey the stream key
     * @param groupName the consumer group name
     */
    @Override
    public void createConsumerGroup(String streamKey, String groupName) {
        consumerGroupPort.createGroup(streamKey, groupName);
    }

    /**
     * Consumes messages from a stream as a named consumer within a group.
     *
     * @param streamKey    the stream key
     * @param groupName    the consumer group name
     * @param consumerName the consumer name
     * @param count        the maximum number of messages to consume
     * @return a list of consumed stream messages
     */
    @Override
    public List<StreamMessage> consumeMessages(String streamKey, String groupName,
                                                String consumerName, int count) {
        return consumerGroupPort.readFromGroup(streamKey, groupName, consumerName, count);
    }

    /**
     * Acknowledges messages as successfully processed.
     *
     * @param streamKey  the stream key
     * @param groupName  the consumer group name
     * @param messageIds the message IDs to acknowledge
     */
    @Override
    public void acknowledgeMessages(String streamKey, String groupName, String... messageIds) {
        consumerGroupPort.acknowledge(streamKey, groupName, messageIds);
    }

    /**
     * Retrieves pending (unacknowledged) messages for a consumer group.
     *
     * @param streamKey the stream key
     * @param groupName the consumer group name
     * @param count     the maximum number of pending entries to return
     * @return a list of pending message details
     */
    @Override
    public List<PendingMessage> getPending(String streamKey, String groupName, int count) {
        return consumerGroupPort.getPendingMessages(streamKey, groupName, count);
    }
}
