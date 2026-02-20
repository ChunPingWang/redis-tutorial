package com.tutorial.redis.module07.domain.port.outbound;

import com.tutorial.redis.module07.domain.model.StreamMessage;

import java.util.List;
import java.util.Map;

/**
 * Outbound port for Redis Stream producer operations.
 * Supports adding entries (XADD), reading entries (XREAD),
 * range queries (XRANGE), and trimming (XTRIM).
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface StreamProducerPort {

    /**
     * Appends a new entry to the specified stream.
     * Equivalent to Redis XADD.
     *
     * @param streamKey the stream key
     * @param fields    the field-value pairs for the entry
     * @return the auto-generated message ID (e.g. "1609459200000-0")
     */
    String addToStream(String streamKey, Map<String, String> fields);

    /**
     * Reads messages from a stream starting after the given message ID.
     * Equivalent to Redis XREAD COUNT.
     *
     * @param streamKey the stream key
     * @param fromId    the message ID to read after (use "0" for all, "$" for new only)
     * @param count     the maximum number of messages to return
     * @return a list of stream messages
     */
    List<StreamMessage> readMessages(String streamKey, String fromId, int count);

    /**
     * Reads messages within a range of message IDs.
     * Equivalent to Redis XRANGE.
     *
     * @param streamKey the stream key
     * @param fromId    the start message ID (inclusive, use "-" for earliest)
     * @param toId      the end message ID (inclusive, use "+" for latest)
     * @return a list of stream messages in the range
     */
    List<StreamMessage> rangeMessages(String streamKey, String fromId, String toId);

    /**
     * Trims the stream to at most the specified length.
     * Equivalent to Redis XTRIM MAXLEN.
     *
     * @param streamKey the stream key
     * @param maxLen    the maximum number of entries to retain
     */
    void trimStream(String streamKey, long maxLen);
}
