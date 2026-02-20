package com.tutorial.redis.module13.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import com.tutorial.redis.module13.domain.service.ProductionChecklistService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module-13 configuration that imports the common Redis configuration and
 * registers domain services as Spring beans.
 *
 * <p>Ensures that {@link RedisConfig} (providing RedisTemplate with
 * Jackson2JsonRedisSerializer and StringRedisTemplate) is available
 * for all adapters in this module.</p>
 *
 * <p>{@link ProductionChecklistService} is a pure domain service with no
 * Spring annotations, so it must be registered explicitly as a bean here
 * to participate in constructor injection.</p>
 */
@Configuration
@Import(RedisConfig.class)
public class Module13Config {

    /**
     * Registers the production checklist domain service as a Spring bean.
     *
     * @return a new instance of the production checklist service
     */
    @Bean
    public ProductionChecklistService productionChecklistService() {
        return new ProductionChecklistService();
    }
}
