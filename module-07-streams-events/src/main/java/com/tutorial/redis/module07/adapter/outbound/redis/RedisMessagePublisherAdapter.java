package com.tutorial.redis.module07.adapter.outbound.redis;

import com.tutorial.redis.module07.domain.port.outbound.MessagePublisherPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub adapter for fire-and-forget message publishing.
 *
 * <p>Implements {@link MessagePublisherPort} using {@link StringRedisTemplate#convertAndSend}
 * which maps directly to the Redis {@code PUBLISH} command. Messages are delivered
 * to all currently subscribed clients on the specified channel; there is no
 * persistence or delivery guarantee.</p>
 */
@Component
public class RedisMessagePublisherAdapter implements MessagePublisherPort {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisMessagePublisherAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Publishes a message to the specified Pub/Sub channel via {@code PUBLISH}.
     *
     * @param channel the Pub/Sub channel name
     * @param message the message payload to publish
     */
    @Override
    public void publish(String channel, String message) {
        stringRedisTemplate.convertAndSend(channel, message);
    }
}
