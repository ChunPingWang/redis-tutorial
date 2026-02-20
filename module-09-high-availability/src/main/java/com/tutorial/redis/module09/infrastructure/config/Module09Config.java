package com.tutorial.redis.module09.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import com.tutorial.redis.module09.domain.service.FailoverProcessService;
import com.tutorial.redis.module09.domain.service.SentinelConfigService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module 09 configuration.
 *
 * <p>Imports the common {@link RedisConfig} to make {@code StringRedisTemplate}
 * available for injection into the replication and failover adapters.</p>
 *
 * <p>Registers the {@link FailoverProcessService} and {@link SentinelConfigService}
 * domain services as Spring beans. These domain services are pure components with
 * no framework dependencies, so they require explicit bean registration rather
 * than component scanning.</p>
 */
@Configuration
@Import(RedisConfig.class)
public class Module09Config {

    /**
     * Registers the {@link FailoverProcessService} as a Spring-managed bean.
     * This domain service generates the educational failover event sequence
     * describing the steps Redis Sentinel takes during automatic failover.
     *
     * @return a new FailoverProcessService instance
     */
    @Bean
    public FailoverProcessService failoverProcessService() {
        return new FailoverProcessService();
    }

    /**
     * Registers the {@link SentinelConfigService} as a Spring-managed bean.
     * This domain service provides recommended Sentinel configuration based
     * on community best practices for a typical three-Sentinel deployment.
     *
     * @return a new SentinelConfigService instance
     */
    @Bean
    public SentinelConfigService sentinelConfigService() {
        return new SentinelConfigService();
    }
}
