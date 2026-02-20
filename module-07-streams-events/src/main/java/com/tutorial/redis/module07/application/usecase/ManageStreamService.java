package com.tutorial.redis.module07.application.usecase;

import com.tutorial.redis.module07.domain.model.StreamMessage;
import com.tutorial.redis.module07.domain.port.inbound.ManageStreamUseCase;
import com.tutorial.redis.module07.domain.port.outbound.StreamProducerPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Application service for Redis Stream management operations.
 *
 * <p>Implements the {@link ManageStreamUseCase} inbound port by delegating
 * to the {@link StreamProducerPort} outbound port. Provides add, read,
 * and trim operations on Redis Streams.</p>
 */
@Service
public class ManageStreamService implements ManageStreamUseCase {

    private final StreamProducerPort streamProducerPort;

    public ManageStreamService(StreamProducerPort streamProducerPort) {
        this.streamProducerPort = streamProducerPort;
    }

    /**
     * Adds a new message to the specified stream.
     *
     * @param streamKey the stream key
     * @param fields    the field-value pairs for the message
     * @return the auto-generated message ID
     */
    @Override
    public String addMessage(String streamKey, Map<String, String> fields) {
        return streamProducerPort.addToStream(streamKey, fields);
    }

    /**
     * Reads messages from a stream starting after the given ID.
     *
     * @param streamKey the stream key
     * @param fromId    the message ID to read after
     * @param count     the maximum number of messages to return
     * @return a list of stream messages
     */
    @Override
    public List<StreamMessage> readMessages(String streamKey, String fromId, int count) {
        return streamProducerPort.readMessages(streamKey, fromId, count);
    }

    /**
     * Trims the stream to at most the specified length.
     *
     * @param streamKey the stream key
     * @param maxLen    the maximum number of entries to retain
     */
    @Override
    public void trimStream(String streamKey, long maxLen) {
        streamProducerPort.trimStream(streamKey, maxLen);
    }
}
