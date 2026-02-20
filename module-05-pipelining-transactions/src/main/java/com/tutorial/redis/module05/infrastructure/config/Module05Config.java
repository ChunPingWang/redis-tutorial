package com.tutorial.redis.module05.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module 05 configuration.
 * Imports the common RedisConfig to make RedisTemplate and StringRedisTemplate
 * available for injection into the pipeline and transaction adapters.
 */
@Configuration
@Import(RedisConfig.class)
public class Module05Config {
}
