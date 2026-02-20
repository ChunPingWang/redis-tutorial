package com.tutorial.redis.module07.domain.port.inbound;

import com.tutorial.redis.module07.domain.model.StreamMessage;

import java.util.List;
import java.util.Map;

/**
 * Inbound port: manage Redis Streams — add, read, and trim entries.
 */
public interface ManageStreamUseCase {

    /**
     * Adds a new message to the specified stream.
     *
     * @param streamKey the stream key
     * @param fields    the field-value pairs for the message
     * @return the auto-generated message ID
     */
    String addMessage(String streamKey, Map<String, String> fields);

    /**
     * Reads messages from a stream starting after the given ID.
     *
     * @param streamKey the stream key
     * @param fromId    the message ID to read after
     * @param count     the maximum number of messages to return
     * @return a list of stream messages
     */
    List<StreamMessage> readMessages(String streamKey, String fromId, int count);

    /**
     * Trims the stream to at most the specified length.
     *
     * @param streamKey the stream key
     * @param maxLen    the maximum number of entries to retain
     */
    void trimStream(String streamKey, long maxLen);
}
