package com.tutorial.redis.common.test;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that require a lightweight Redis instance.
 *
 * <p>Uses {@code redis:8-alpine} which provides core Redis functionality
 * without additional modules. Suitable for tests that only need basic
 * Redis data structures and commands.</p>
 *
 * <p>The Redis instance is automatically flushed before each test to ensure
 * test isolation.</p>
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractRedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

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
