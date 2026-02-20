package com.tutorial.redis.module07.domain.port.outbound;

/**
 * Outbound port for Redis Pub/Sub message publishing.
 * Publishes fire-and-forget messages to a named channel.
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface MessagePublisherPort {

    /**
     * Publishes a message to the specified Pub/Sub channel.
     * This is a fire-and-forget operation — no delivery guarantee.
     *
     * @param channel the Pub/Sub channel name
     * @param message the message payload to publish
     */
    void publish(String channel, String message);
}
