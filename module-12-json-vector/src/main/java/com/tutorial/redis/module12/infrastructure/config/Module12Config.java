package com.tutorial.redis.module12.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module-12 configuration that imports the common Redis configuration.
 *
 * <p>Ensures that {@link RedisConfig} (providing RedisTemplate with
 * Jackson2JsonRedisSerializer and the shared ObjectMapper) is available
 * for all adapters in this module.</p>
 *
 * <p>This module uses {@code StringRedisTemplate} for all operations.
 * RedisJSON module commands (JSON.*) are invoked via Lua scripts
 * (DefaultRedisScript) since {@code connection.execute()} does not work
 * with Lettuce / Spring Data Redis 4.x for module commands.
 * Requires Redis Stack (redis/redis-stack image) for RedisJSON and
 * RediSearch (vector search index creation) support.</p>
 *
 * <p>Jackson {@link com.fasterxml.jackson.databind.ObjectMapper} is provided
 * by Spring Boot auto-configuration and injected into application services
 * for JSON serialization/deserialization of product documents.</p>
 */
@Configuration
@Import(RedisConfig.class)
public class Module12Config {
}
