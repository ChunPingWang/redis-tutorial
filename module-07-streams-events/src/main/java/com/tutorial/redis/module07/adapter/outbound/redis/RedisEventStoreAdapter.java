package com.tutorial.redis.module07.adapter.outbound.redis;

import com.tutorial.redis.module07.domain.model.AccountEvent;
import com.tutorial.redis.module07.domain.port.outbound.EventStorePort;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream-based event store adapter for event sourcing.
 *
 * <p>Implements {@link EventStorePort} by storing domain events as Redis Stream
 * entries. Each {@link AccountEvent} is serialized into a flat {@code Map<String, String>}
 * payload for XADD, and deserialized back from XRANGE results.</p>
 *
 * <p>Event stream key format: {@code event:account:{accountId}}</p>
 *
 * <p>Stored fields per entry:</p>
 * <ul>
 *   <li>{@code eventType} — the event type (e.g. ACCOUNT_OPENED, MONEY_DEPOSITED)</li>
 *   <li>{@code accountId} — the account identifier</li>
 *   <li>{@code amount} — the monetary amount (may be empty if null)</li>
 *   <li>{@code timestamp} — the event timestamp as ISO-8601 string</li>
 *   <li>Any additional metadata key-value pairs</li>
 * </ul>
 */
@Component
public class RedisEventStoreAdapter implements EventStorePort {

    private static final String STREAM_KEY_PREFIX = "event:account:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisEventStoreAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Appends an account event to the specified event stream via {@code XADD}.
     * The event is converted to a flat map of string field-value pairs.
     *
     * @param streamKey the stream key (e.g. "event:account:acc-001")
     * @param event     the account event to append
     * @return the auto-generated event ID assigned by Redis
     */
    @Override
    public String appendEvent(String streamKey, AccountEvent event) {
        Map<String, String> fields = eventToMap(event);

        MapRecord<String, String, String> record = StreamRecords
                .<String, String, String>mapBacked(fields)
                .withStreamKey(streamKey);

        return stringRedisTemplate.opsForStream()
                .add(record)
                .getValue();
    }

    /**
     * Reads all events from the specified event stream via {@code XRANGE - +}.
     * Used for full state reconstruction through event replay.
     *
     * @param streamKey the stream key
     * @return all account events in chronological order
     */
    @Override
    public List<AccountEvent> readAllEvents(String streamKey) {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                .range(streamKey, Range.unbounded());

        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        return records.stream()
                .map(this::mapToAccountEvent)
                .toList();
    }

    /**
     * Reads events from the specified stream starting from the given event ID
     * via {@code XRANGE fromEventId +}. Used for partial replay / catch-up scenarios.
     *
     * @param streamKey   the stream key
     * @param fromEventId the event ID to read from (inclusive)
     * @return account events from the given ID onward in chronological order
     */
    @Override
    public List<AccountEvent> readEventsFrom(String streamKey, String fromEventId) {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                .range(streamKey, Range.rightUnbounded(Range.Bound.inclusive(fromEventId)));

        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        return records.stream()
                .map(this::mapToAccountEvent)
                .toList();
    }

    /**
     * Converts an {@link AccountEvent} to a flat map of string field-value pairs
     * for storage as a Redis Stream entry.
     */
    private Map<String, String> eventToMap(AccountEvent event) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("eventType", event.getEventType());
        fields.put("accountId", event.getAccountId());
        fields.put("amount", event.getAmount() != null ? String.valueOf(event.getAmount()) : "");
        fields.put("timestamp", event.getTimestamp().toString());

        // Include metadata entries as additional fields
        if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
            event.getMetadata().forEach((key, value) ->
                    fields.put("meta:" + key, value));
        }

        return fields;
    }

    /**
     * Converts a Redis Stream {@link MapRecord} back into an {@link AccountEvent}.
     * The Redis-assigned message ID becomes the event ID.
     */
    private AccountEvent mapToAccountEvent(MapRecord<String, Object, Object> record) {
        String eventId = record.getId().getValue();

        Map<Object, Object> values = record.getValue();
        String eventType = String.valueOf(values.get("eventType"));
        String accountId = String.valueOf(values.get("accountId"));

        String amountStr = String.valueOf(values.get("amount"));
        Double amount = (amountStr == null || amountStr.isEmpty() || "null".equals(amountStr))
                ? null
                : Double.parseDouble(amountStr);

        String timestampStr = String.valueOf(values.get("timestamp"));
        Instant timestamp = Instant.parse(timestampStr);

        // Extract metadata entries (prefixed with "meta:")
        Map<String, String> metadata = new HashMap<>();
        values.forEach((key, value) -> {
            String keyStr = String.valueOf(key);
            if (keyStr.startsWith("meta:")) {
                metadata.put(keyStr.substring(5), String.valueOf(value));
            }
        });

        return new AccountEvent(eventId, accountId, eventType, amount, timestamp, metadata);
    }
}
