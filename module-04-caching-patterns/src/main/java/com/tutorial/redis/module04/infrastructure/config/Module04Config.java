package com.tutorial.redis.module04.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import com.tutorial.redis.module04.domain.service.CacheTtlService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module 04 configuration.
 * Imports the common RedisConfig to make RedisTemplate and StringRedisTemplate
 * available for injection, and registers domain services as beans.
 */
@Configuration
@Import(RedisConfig.class)
public class Module04Config {

    @Bean
    public CacheTtlService cacheTtlService() {
        return new CacheTtlService();
    }
}
