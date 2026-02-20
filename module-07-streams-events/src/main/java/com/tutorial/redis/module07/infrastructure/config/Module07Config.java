package com.tutorial.redis.module07.infrastructure.config;

import com.tutorial.redis.common.config.RedisConfig;
import com.tutorial.redis.module07.domain.service.EventReplayService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Module 07 configuration.
 *
 * <p>Imports the common {@link RedisConfig} to make {@code StringRedisTemplate}
 * available for injection into the stream and event store adapters.</p>
 *
 * <p>Registers the {@link EventReplayService} domain service as a Spring bean.
 * The replay service is a pure domain component with no framework dependencies,
 * so it requires explicit bean registration rather than component scanning.</p>
 */
@Configuration
@Import(RedisConfig.class)
public class Module07Config {

    /**
     * Registers the {@link EventReplayService} as a Spring-managed bean.
     * This domain service contains the event replay fold logic used by
     * {@link com.tutorial.redis.module07.application.usecase.EventSourcingService}
     * to reconstruct account state from event streams.
     *
     * @return a new EventReplayService instance
     */
    @Bean
    public EventReplayService eventReplayService() {
        return new EventReplayService();
    }
}
