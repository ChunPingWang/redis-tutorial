package com.tutorial.redis.module07.domain.port.outbound;

import com.tutorial.redis.module07.domain.model.PendingMessage;
import com.tutorial.redis.module07.domain.model.StreamMessage;

import java.util.List;

/**
 * Outbound port for Redis Stream consumer group operations.
 * Supports group creation (XGROUP CREATE), reading (XREADGROUP),
 * acknowledgment (XACK), pending inspection (XPENDING),
 * and message claiming (XCLAIM).
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface ConsumerGroupPort {

    /**
     * Creates a consumer group for the specified stream.
     * Equivalent to Redis XGROUP CREATE.
     *
     * @param streamKey the stream key
     * @param groupName the consumer group name
     */
    void createGroup(String streamKey, String groupName);

    /**
     * Reads messages from a stream as a consumer within a group.
     * Equivalent to Redis XREADGROUP GROUP groupName consumerName COUNT count.
     *
     * @param streamKey    the stream key
     * @param groupName    the consumer group name
     * @param consumerName the consumer name within the group
     * @param count        the maximum number of messages to return
     * @return a list of stream messages delivered to this consumer
     */
    List<StreamMessage> readFromGroup(String streamKey, String groupName,
                                      String consumerName, int count);

    /**
     * Acknowledges one or more messages as successfully processed.
     * Equivalent to Redis XACK.
     *
     * @param streamKey  the stream key
     * @param groupName  the consumer group name
     * @param messageIds the message IDs to acknowledge
     */
    void acknowledge(String streamKey, String groupName, String... messageIds);

    /**
     * Retrieves pending messages for a consumer group.
     * These are messages delivered but not yet acknowledged.
     * Equivalent to Redis XPENDING.
     *
     * @param streamKey the stream key
     * @param groupName the consumer group name
     * @param count     the maximum number of pending entries to return
     * @return a list of pending message details
     */
    List<PendingMessage> getPendingMessages(String streamKey, String groupName, int count);

    /**
     * Claims idle messages from other consumers in the group.
     * Transfers ownership of messages that have been pending longer
     * than the specified idle time. Equivalent to Redis XCLAIM.
     *
     * @param streamKey     the stream key
     * @param groupName     the consumer group name
     * @param consumerName  the consumer claiming the messages
     * @param minIdleTimeMs minimum idle time in milliseconds
     * @param messageIds    the message IDs to claim
     * @return a list of successfully claimed stream messages
     */
    List<StreamMessage> claimMessages(String streamKey, String groupName,
                                      String consumerName, long minIdleTimeMs,
                                      String... messageIds);
}
