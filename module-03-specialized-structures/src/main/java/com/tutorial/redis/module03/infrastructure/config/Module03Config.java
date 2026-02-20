package com.tutorial.redis.module03.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module-03 configuration that imports the common Redis configuration.
 *
 * <p>Ensures that {@link RedisConfig} (providing RedisTemplate with
 * Jackson2JsonRedisSerializer and NON_FINAL default typing) is available
 * for all adapters in this module.</p>
 *
 * <p>This module uses both {@code RedisTemplate<String, Object>} (for Geo operations)
 * and {@code StringRedisTemplate} (for Bitmap, HyperLogLog, and module commands).
 * Module commands (BF.*, CF.*, TS.*) require Redis Stack (redis/redis-stack image).</p>
 */
@Configuration
@Import(RedisConfig.class)
public class Module03Config {
}
