package com.tutorial.redis.module07.adapter.outbound.redis;

import com.tutorial.redis.module07.domain.model.PendingMessage;
import com.tutorial.redis.module07.domain.model.StreamMessage;
import com.tutorial.redis.module07.domain.port.outbound.ConsumerGroupPort;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream consumer group adapter for group-based consumption.
 *
 * <p>Implements {@link ConsumerGroupPort} using Spring Data Redis
 * {@code StreamOperations}, providing XGROUP CREATE, XREADGROUP, XACK,
 * XPENDING, and XCLAIM functionality.</p>
 *
 * <p>Consumer groups enable parallel processing of stream entries across
 * multiple consumers, with at-least-once delivery guarantees through
 * the pending entries list (PEL) and explicit acknowledgment.</p>
 */
@Component
public class RedisConsumerGroupAdapter implements ConsumerGroupPort {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisConsumerGroupAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Creates a consumer group for the specified stream via {@code XGROUP CREATE}.
     * If the group already exists (BUSYGROUP error), the error is silently ignored.
     *
     * @param streamKey the stream key
     * @param groupName the consumer group name
     */
    @Override
    public void createGroup(String streamKey, String groupName) {
        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), groupName);
        } catch (Exception e) {
            // BUSYGROUP: Consumer Group name already exists
            if (!containsBusyGroup(e)) {
                throw e;
            }
        }
    }

    /**
     * Checks whether an exception (or any of its causes) contains the BUSYGROUP error,
     * indicating the consumer group already exists.
     */
    private boolean containsBusyGroup(Throwable t) {
        while (t != null) {
            if (t.getMessage() != null && t.getMessage().contains("BUSYGROUP")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * Reads messages from a stream as a consumer within a group via {@code XREADGROUP}.
     * Uses {@code ReadOffset.lastConsumed()} to read only new (undelivered) messages.
     *
     * @param streamKey    the stream key
     * @param groupName    the consumer group name
     * @param consumerName the consumer name within the group
     * @param count        the maximum number of messages to return
     * @return a list of stream messages delivered to this consumer
     */
    @Override
    public List<StreamMessage> readFromGroup(String streamKey, String groupName,
                                              String consumerName, int count) {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                .read(Consumer.from(groupName, consumerName),
                        StreamReadOptions.empty().count(count),
                        StreamOffset.create(streamKey, ReadOffset.lastConsumed()));

        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        return records.stream()
                .map(record -> mapToStreamMessage(record, streamKey))
                .toList();
    }

    /**
     * Acknowledges one or more messages as successfully processed via {@code XACK}.
     *
     * @param streamKey  the stream key
     * @param groupName  the consumer group name
     * @param messageIds the message IDs to acknowledge
     */
    @Override
    public void acknowledge(String streamKey, String groupName, String... messageIds) {
        stringRedisTemplate.opsForStream().acknowledge(streamKey, groupName, messageIds);
    }

    /**
     * Retrieves pending messages for a consumer group via {@code XPENDING}.
     * Returns details about messages that have been delivered but not yet acknowledged.
     *
     * @param streamKey the stream key
     * @param groupName the consumer group name
     * @param count     the maximum number of pending entries to return
     * @return a list of pending message details
     */
    @Override
    public List<PendingMessage> getPendingMessages(String streamKey, String groupName, int count) {
        PendingMessages pendingMessages = stringRedisTemplate.opsForStream()
                .pending(streamKey, groupName, Range.unbounded(), count);

        List<PendingMessage> result = new ArrayList<>();
        pendingMessages.forEach(message -> result.add(new PendingMessage(
                message.getId().getValue(),
                message.getConsumerName(),
                message.getElapsedTimeSinceLastDelivery().toMillis(),
                message.getTotalDeliveryCount()
        )));

        return result;
    }

    /**
     * Claims idle messages from other consumers in the group via {@code XCLAIM}.
     * Transfers ownership of messages that have been pending longer than the
     * specified idle time.
     *
     * @param streamKey     the stream key
     * @param groupName     the consumer group name
     * @param consumerName  the consumer claiming the messages
     * @param minIdleTimeMs minimum idle time in milliseconds
     * @param messageIds    the message IDs to claim
     * @return a list of successfully claimed stream messages
     */
    @Override
    public List<StreamMessage> claimMessages(String streamKey, String groupName,
                                              String consumerName, long minIdleTimeMs,
                                              String... messageIds) {
        RecordId[] recordIds = Arrays.stream(messageIds)
                .map(RecordId::of)
                .toArray(RecordId[]::new);

        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                .claim(streamKey, groupName, consumerName,
                        Duration.ofMillis(minIdleTimeMs), recordIds);

        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        return records.stream()
                .map(record -> mapToStreamMessage(record, streamKey))
                .toList();
    }

    /**
     * Converts a Spring Data Redis {@link MapRecord} into a domain {@link StreamMessage}.
     */
    private StreamMessage mapToStreamMessage(MapRecord<String, Object, Object> record, String streamKey) {
        String messageId = record.getId().getValue();

        Map<String, String> payload = new LinkedHashMap<>();
        record.getValue().forEach((key, value) ->
                payload.put(String.valueOf(key), String.valueOf(value)));

        Instant timestamp = extractTimestamp(messageId);

        return new StreamMessage(messageId, streamKey, payload, timestamp);
    }

    /**
     * Extracts the timestamp from a Redis Stream message ID.
     * Message IDs have the format {@code <millisecondsTime>-<sequenceNumber>}.
     */
    private Instant extractTimestamp(String messageId) {
        try {
            String millisPart = messageId.split("-")[0];
            return Instant.ofEpochMilli(Long.parseLong(millisPart));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return Instant.now();
        }
    }
}
