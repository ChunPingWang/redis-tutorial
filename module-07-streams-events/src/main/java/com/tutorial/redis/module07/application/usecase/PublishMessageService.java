package com.tutorial.redis.module07.application.usecase;

import com.tutorial.redis.module07.domain.port.inbound.PublishMessageUseCase;
import com.tutorial.redis.module07.domain.port.outbound.MessagePublisherPort;
import org.springframework.stereotype.Service;

/**
 * Application service for Redis Pub/Sub message publishing.
 *
 * <p>Implements the {@link PublishMessageUseCase} inbound port by delegating
 * to the {@link MessagePublisherPort} outbound port. This thin orchestration
 * layer sits between the REST controller and the Redis adapter, preserving
 * the hexagonal architecture boundary.</p>
 */
@Service
public class PublishMessageService implements PublishMessageUseCase {

    private final MessagePublisherPort messagePublisherPort;

    public PublishMessageService(MessagePublisherPort messagePublisherPort) {
        this.messagePublisherPort = messagePublisherPort;
    }

    /**
     * Publishes a message to the specified Pub/Sub channel.
     *
     * @param channel the channel name to publish to
     * @param message the message payload
     */
    @Override
    public void publish(String channel, String message) {
        messagePublisherPort.publish(channel, message);
    }
}
