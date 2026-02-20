package com.tutorial.redis.module14.ecommerce.adapter.outbound.redis;

import com.tutorial.redis.module14.ecommerce.domain.port.outbound.GeoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Redis adapter for geospatial operations.
 *
 * <p>Implements {@link GeoPort} using Redis GEO commands via
 * {@link StringRedisTemplate}. Stores locations with longitude/latitude
 * and supports radius-based queries.</p>
 */
@Component
public class RedisGeoAdapter implements GeoPort {

    private static final Logger log = LoggerFactory.getLogger(RedisGeoAdapter.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisGeoAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void addLocation(String key, double longitude, double latitude, String member) {
        log.debug("Adding location {} at ({}, {}) to key {}", member, longitude, latitude, key);
        stringRedisTemplate.opsForGeo().add(key, new Point(longitude, latitude), member);
    }

    @Override
    public List<String> findNearby(String key, double longitude, double latitude,
                                   double radiusKm) {
        log.debug("Finding members near ({}, {}) within {} km in key {}",
                longitude, latitude, radiusKm, key);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                stringRedisTemplate.opsForGeo().radius(key,
                        new Circle(new Point(longitude, latitude),
                                new Distance(radiusKm, Metrics.KILOMETERS)));

        if (results == null) {
            return Collections.emptyList();
        }

        List<String> members = new ArrayList<>();
        results.getContent().forEach(geoResult ->
                members.add(geoResult.getContent().getName()));
        return members;
    }
}
