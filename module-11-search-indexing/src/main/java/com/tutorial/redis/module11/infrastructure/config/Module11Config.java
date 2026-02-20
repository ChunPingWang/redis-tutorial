package com.tutorial.redis.module11.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module-11 configuration that imports the common Redis configuration.
 *
 * <p>Ensures that {@link RedisConfig} (providing RedisTemplate with
 * Jackson2JsonRedisSerializer and NON_FINAL default typing) is available
 * for all adapters in this module.</p>
 *
 * <p>This module uses {@code StringRedisTemplate} for all operations.
 * RediSearch module commands (FT.*) are invoked via Lua scripts
 * (DefaultRedisScript) since connection.execute() does not work
 * with Lettuce / Spring Data Redis 4.x for module commands.
 * Requires Redis Stack (redis/redis-stack image) for RediSearch support.</p>
 */
@Configuration
@Import(RedisConfig.class)
public class Module11Config {
}
