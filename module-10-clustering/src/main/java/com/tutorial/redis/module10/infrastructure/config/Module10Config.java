package com.tutorial.redis.module10.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import com.tutorial.redis.module10.domain.service.ClusterTopologyService;
import com.tutorial.redis.module10.domain.service.HashSlotCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module 10 configuration.
 *
 * <p>Imports the common {@link RedisConfig} to make {@code StringRedisTemplate}
 * available for injection into the cluster data adapter.</p>
 *
 * <p>Registers the {@link HashSlotCalculator} and {@link ClusterTopologyService}
 * domain services as Spring beans. These domain services are pure components with
 * no framework dependencies, so they require explicit bean registration rather
 * than component scanning.</p>
 */
@Configuration
@Import(RedisConfig.class)
public class Module10Config {

    /**
     * Registers the {@link HashSlotCalculator} as a Spring-managed bean.
     * This domain service performs CRC16-based hash slot calculations
     * following the Redis Cluster specification.
     *
     * @return a new HashSlotCalculator instance
     */
    @Bean
    public HashSlotCalculator hashSlotCalculator() {
        return new HashSlotCalculator();
    }

    /**
     * Registers the {@link ClusterTopologyService} as a Spring-managed bean.
     * This domain service generates educational cluster topology configurations
     * with balanced hash slot distribution across master nodes.
     *
     * @return a new ClusterTopologyService instance
     */
    @Bean
    public ClusterTopologyService clusterTopologyService() {
        return new ClusterTopologyService();
    }
}
