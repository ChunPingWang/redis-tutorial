package com.tutorial.redis.module06.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module 06 configuration.
 * Imports the common RedisConfig to make {@code RedisTemplate<String, Object>}
 * and {@code StringRedisTemplate} available for injection into the data modeling adapters.
 *
 * <p>The RedisTemplate uses Jackson serialization with NON_FINAL default typing,
 * enabling automatic serialization/deserialization of domain objects (e.g., Order)
 * stored as JSON Strings. The StringRedisTemplate is used for Hash fields and
 * secondary index operations where plain string values are preferred.</p>
 */
@Configuration
@Import(RedisConfig.class)
public class Module06Config {
}
