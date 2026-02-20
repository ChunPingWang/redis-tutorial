package com.tutorial.redis.common.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Base class for integration tests that require a lightweight Redis instance.
 *
 * <p>Uses {@code redis:8-alpine} which provides core Redis functionality
 * without additional modules. Suitable for tests that only need basic
 * Redis data structures and commands.</p>
 *
 * <p>Uses the singleton container pattern so the same Redis instance is
 * shared across all test classes, avoiding Spring context caching issues.</p>
 *
 * <p>The Redis instance is automatically flushed before each test to ensure
 * test isolation.</p>
 */
@SpringBootTest
public abstract class AbstractRedisIntegrationTest {

    static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    static {
        redis.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void flushRedis() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }
}
