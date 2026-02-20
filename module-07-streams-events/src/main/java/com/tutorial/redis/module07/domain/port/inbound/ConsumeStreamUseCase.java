package com.tutorial.redis.module07.domain.port.inbound;

import com.tutorial.redis.module07.domain.model.PendingMessage;
import com.tutorial.redis.module07.domain.model.StreamMessage;

import java.util.List;

/**
 * Inbound port: consume messages from Redis Streams using consumer groups.
 * Supports group creation, message consumption, acknowledgment,
 * and pending message inspection.
 */
public interface ConsumeStreamUseCase {

    /**
     * Creates a consumer group for the specified stream.
     *
     * @param streamKey the stream key
     * @param groupName the consumer group name
     */
    void createConsumerGroup(String streamKey, String groupName);

    /**
     * Consumes messages from a stream as a named consumer within a group.
     *
     * @param streamKey    the stream key
     * @param groupName    the consumer group name
     * @param consumerName the consumer name
     * @param count        the maximum number of messages to consume
     * @return a list of consumed stream messages
     */
    List<StreamMessage> consumeMessages(String streamKey, String groupName,
                                        String consumerName, int count);

    /**
     * Acknowledges messages as successfully processed.
     *
     * @param streamKey  the stream key
     * @param groupName  the consumer group name
     * @param messageIds the message IDs to acknowledge
     */
    void acknowledgeMessages(String streamKey, String groupName, String... messageIds);

    /**
     * Retrieves pending (unacknowledged) messages for a consumer group.
     *
     * @param streamKey the stream key
     * @param groupName the consumer group name
     * @param count     the maximum number of pending entries to return
     * @return a list of pending message details
     */
    List<PendingMessage> getPending(String streamKey, String groupName, int count);
}
