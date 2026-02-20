package com.tutorial.redis.module08.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import com.tutorial.redis.module08.domain.service.RpoRtoAnalysisService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module 08 configuration.
 *
 * <p>Imports the common {@link RedisConfig} to make {@code StringRedisTemplate}
 * available for injection into the persistence and recovery adapters.</p>
 *
 * <p>Registers the {@link RpoRtoAnalysisService} domain service as a Spring bean.
 * The analysis service is a pure domain component with no framework dependencies,
 * so it requires explicit bean registration rather than component scanning.</p>
 */
@Configuration
@Import(RedisConfig.class)
public class Module08Config {

    /**
     * Registers the {@link RpoRtoAnalysisService} as a Spring-managed bean.
     * This domain service contains pre-defined RPO/RTO analysis data for each
     * Redis persistence strategy and is used by
     * {@link com.tutorial.redis.module08.application.usecase.RpoRtoAnalysisApplicationService}
     * to serve analysis requests.
     *
     * @return a new RpoRtoAnalysisService instance
     */
    @Bean
    public RpoRtoAnalysisService rpoRtoAnalysisService() {
        return new RpoRtoAnalysisService();
    }
}
