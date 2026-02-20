package com.tutorial.redis.module01.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(RedisConfig.class)
public class Module01Config {
}
