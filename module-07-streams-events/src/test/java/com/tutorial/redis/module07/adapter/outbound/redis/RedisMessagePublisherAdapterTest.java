package com.tutorial.redis.module07.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisMessagePublisherAdapter 整合測試")
class RedisMessagePublisherAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisMessagePublisherAdapter publisherAdapter;

    @Test
    @DisplayName("publish_SendsMessageToChannel — 發布消息到 Pub/Sub 頻道後，訂閱者應收到正確訊息")
    void publish_SendsMessageToChannel() throws InterruptedException {
        // Arrange
        var received = new AtomicReference<String>();
        var latch = new CountDownLatch(1);

        Thread subscriber = new Thread(() -> {
            stringRedisTemplate.getConnectionFactory().getConnection()
                    .subscribe((message, pattern) -> {
                        received.set(new String(message.getBody()));
                        latch.countDown();
                    }, "test-channel".getBytes());
        });
        subscriber.setDaemon(true);
        subscriber.start();

        // Wait for the subscription to be established
        Thread.sleep(200);

        // Act
        publisherAdapter.publish("test-channel", "hello");

        // Assert
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(received.get()).isEqualTo("hello");
    }
}
