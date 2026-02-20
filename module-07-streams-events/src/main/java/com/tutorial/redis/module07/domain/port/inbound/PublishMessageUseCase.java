package com.tutorial.redis.module07.domain.port.inbound;

/**
 * Inbound port: publish messages via Redis Pub/Sub.
 * Fire-and-forget messaging to named channels.
 */
public interface PublishMessageUseCase {

    /**
     * Publishes a message to the specified Pub/Sub channel.
     *
     * @param channel the channel name to publish to
     * @param message the message payload
     */
    void publish(String channel, String message);
}
