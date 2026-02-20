package com.tutorial.redis.module07.adapter.inbound.rest;

import com.tutorial.redis.module07.domain.model.AccountEvent;
import com.tutorial.redis.module07.domain.model.AccountState;
import com.tutorial.redis.module07.domain.model.PendingMessage;
import com.tutorial.redis.module07.domain.model.StreamMessage;
import com.tutorial.redis.module07.domain.port.inbound.ConsumeStreamUseCase;
import com.tutorial.redis.module07.domain.port.inbound.EventSourcingUseCase;
import com.tutorial.redis.module07.domain.port.inbound.ManageStreamUseCase;
import com.tutorial.redis.module07.domain.port.inbound.PublishMessageUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing endpoints for Redis Streams and event-driven patterns:
 * <ul>
 *   <li>Pub/Sub message publishing</li>
 *   <li>Stream message management (add, read, trim)</li>
 *   <li>Consumer group operations (create, consume, acknowledge, pending)</li>
 *   <li>Event sourcing (append events, replay state, get event history)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/streams")
public class StreamsController {

    private final PublishMessageUseCase publishMessageUseCase;
    private final ManageStreamUseCase manageStreamUseCase;
    private final ConsumeStreamUseCase consumeStreamUseCase;
    private final EventSourcingUseCase eventSourcingUseCase;

    public StreamsController(PublishMessageUseCase publishMessageUseCase,
                             ManageStreamUseCase manageStreamUseCase,
                             ConsumeStreamUseCase consumeStreamUseCase,
                             EventSourcingUseCase eventSourcingUseCase) {
        this.publishMessageUseCase = publishMessageUseCase;
        this.manageStreamUseCase = manageStreamUseCase;
        this.consumeStreamUseCase = consumeStreamUseCase;
        this.eventSourcingUseCase = eventSourcingUseCase;
    }

    // ===================== Pub/Sub Endpoints =====================

    /**
     * Publishes a fire-and-forget message to a Redis Pub/Sub channel.
     *
     * @param channel the channel name
     * @param message the message payload
     * @return confirmation with channel and message details
     */
    @PostMapping("/publish")
    public ResponseEntity<Map<String, String>> publish(@RequestParam String channel,
                                                        @RequestParam String message) {
        publishMessageUseCase.publish(channel, message);
        return ResponseEntity.ok(Map.of(
                "status", "published",
                "channel", channel,
                "message", message
        ));
    }

    // ===================== Stream Message Endpoints =====================

    /**
     * Adds a new message to the specified stream via XADD.
     *
     * @param streamKey the stream key
     * @param fields    the field-value pairs for the message body
     * @return the auto-generated message ID
     */
    @PostMapping("/messages/{streamKey}")
    public ResponseEntity<Map<String, String>> addMessage(@PathVariable String streamKey,
                                                           @RequestBody Map<String, String> fields) {
        String messageId = manageStreamUseCase.addMessage(streamKey, fields);
        return ResponseEntity.ok(Map.of(
                "status", "added",
                "streamKey", streamKey,
                "messageId", messageId
        ));
    }

    /**
     * Reads messages from a stream starting after the given ID via XREAD.
     *
     * @param streamKey the stream key
     * @param fromId    the message ID to read after (default "0-0" for all)
     * @param count     the maximum number of messages to return (default 10)
     * @return a list of stream messages
     */
    @GetMapping("/messages/{streamKey}")
    public ResponseEntity<List<StreamMessage>> readMessages(
            @PathVariable String streamKey,
            @RequestParam(defaultValue = "0-0") String fromId,
            @RequestParam(defaultValue = "10") int count) {
        List<StreamMessage> messages = manageStreamUseCase.readMessages(streamKey, fromId, count);
        return ResponseEntity.ok(messages);
    }

    // ===================== Consumer Group Endpoints =====================

    /**
     * Creates a consumer group for the specified stream via XGROUP CREATE.
     *
     * @param streamKey the stream key
     * @param groupName the consumer group name
     * @return confirmation of group creation
     */
    @PostMapping("/groups/{streamKey}/{groupName}")
    public ResponseEntity<Map<String, String>> createConsumerGroup(@PathVariable String streamKey,
                                                                    @PathVariable String groupName) {
        consumeStreamUseCase.createConsumerGroup(streamKey, groupName);
        return ResponseEntity.ok(Map.of(
                "status", "created",
                "streamKey", streamKey,
                "groupName", groupName
        ));
    }

    /**
     * Consumes messages from a stream as a named consumer within a group via XREADGROUP.
     *
     * @param streamKey    the stream key
     * @param groupName    the consumer group name
     * @param consumerName the consumer name
     * @param count        the maximum number of messages to consume (default 10)
     * @return a list of consumed stream messages
     */
    @GetMapping("/groups/{streamKey}/{groupName}/{consumerName}")
    public ResponseEntity<List<StreamMessage>> consumeMessages(
            @PathVariable String streamKey,
            @PathVariable String groupName,
            @PathVariable String consumerName,
            @RequestParam(defaultValue = "10") int count) {
        List<StreamMessage> messages = consumeStreamUseCase.consumeMessages(
                streamKey, groupName, consumerName, count);
        return ResponseEntity.ok(messages);
    }

    /**
     * Acknowledges one or more messages as successfully processed via XACK.
     *
     * @param streamKey  the stream key
     * @param groupName  the consumer group name
     * @param messageIds the list of message IDs to acknowledge
     * @return confirmation of acknowledgment
     */
    @PostMapping("/groups/{streamKey}/{groupName}/ack")
    public ResponseEntity<Map<String, Object>> acknowledgeMessages(
            @PathVariable String streamKey,
            @PathVariable String groupName,
            @RequestBody List<String> messageIds) {
        consumeStreamUseCase.acknowledgeMessages(streamKey, groupName,
                messageIds.toArray(new String[0]));
        return ResponseEntity.ok(Map.of(
                "status", "acknowledged",
                "streamKey", streamKey,
                "groupName", groupName,
                "messageIds", messageIds
        ));
    }

    // ===================== Event Sourcing Endpoints =====================

    /**
     * Appends an account event to the event store via XADD.
     *
     * @param event the account event to append
     * @return the auto-generated event ID
     */
    @PostMapping("/events")
    public ResponseEntity<Map<String, String>> appendAccountEvent(@RequestBody AccountEvent event) {
        String eventId = eventSourcingUseCase.appendAccountEvent(event);
        return ResponseEntity.ok(Map.of(
                "status", "appended",
                "eventId", eventId,
                "accountId", event.getAccountId(),
                "eventType", event.getEventType()
        ));
    }

    /**
     * Replays all events for the given account to reconstruct its current state.
     *
     * @param accountId the account identifier
     * @return the reconstructed account state
     */
    @GetMapping("/events/{accountId}/replay")
    public ResponseEntity<AccountState> replayEvents(@PathVariable String accountId) {
        AccountState state = eventSourcingUseCase.replayEvents(accountId);
        return ResponseEntity.ok(state);
    }

    /**
     * Retrieves all events for the given account in chronological order.
     *
     * @param accountId the account identifier
     * @return a list of account events
     */
    @GetMapping("/events/{accountId}")
    public ResponseEntity<List<AccountEvent>> getEvents(@PathVariable String accountId) {
        List<AccountEvent> events = eventSourcingUseCase.getEvents(accountId);
        return ResponseEntity.ok(events);
    }
}
