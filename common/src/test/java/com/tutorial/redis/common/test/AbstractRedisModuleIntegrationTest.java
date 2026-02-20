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
 * Base class for integration tests that require a full Redis 8 instance with modules.
 *
 * <p>Uses {@code redis:8} which includes all Redis modules such as
 * RediSearch, RedisJSON, RedisBloom, and RedisTimeSeries. Suitable for
 * tests that exercise module-specific commands and functionality.</p>
 *
 * <p>The Redis instance is automatically flushed before each test to ensure
 * test isolation.</p>
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractRedisModuleIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:8")
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
