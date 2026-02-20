package com.tutorial.redis.module02.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module-02 configuration that imports the common Redis configuration.
 *
 * <p>Ensures that {@link RedisConfig} (providing RedisTemplate with
 * Jackson2JsonRedisSerializer and NON_FINAL default typing) is available
 * for all adapters in this module.</p>
 */
@Configuration
@Import(RedisConfig.class)
public class Module02Config {
}
