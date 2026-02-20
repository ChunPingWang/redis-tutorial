package com.tutorial.redis.module03.adapter.outbound.redis;

import com.tutorial.redis.common.config.RedisKeyConvention;
import com.tutorial.redis.module03.domain.model.GeoSearchResult;
import com.tutorial.redis.module03.domain.model.StoreLocation;
import com.tutorial.redis.module03.domain.port.outbound.GeoLocationPort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Redis adapter for geospatial location operations using GeoOperations.
 *
 * <p>Stores all store locations in a single Geo key using {@code opsForGeo()}.
 * Store metadata (name) is stored separately in a Redis Hash per store.</p>
 *
 * <p>Geo key: {@code ecommerce:store:locations}</p>
 * <p>Metadata key pattern: {@code ecommerce:store:meta:{storeId}}</p>
 */
@Component
public class RedisGeoLocationAdapter implements GeoLocationPort {

    private static final String SERVICE = "ecommerce";
    private static final String ENTITY = "store";
    private static final String GEO_KEY = SERVICE + ":store:locations";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisGeoLocationAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void addLocation(StoreLocation location) {
        redisTemplate.opsForGeo().add(GEO_KEY,
                new Point(location.getLongitude(), location.getLatitude()),
                location.getStoreId());

        String metaKey = buildMetaKey(location.getStoreId());
        redisTemplate.opsForHash().put(metaKey, "name", location.getName());
    }

    @Override
    public void addLocations(List<StoreLocation> locations) {
        for (StoreLocation location : locations) {
            addLocation(location);
        }
    }

    @Override
    public Optional<StoreLocation> getPosition(String storeId) {
        List<Point> positions = redisTemplate.opsForGeo().position(GEO_KEY, storeId);
        if (positions == null || positions.isEmpty() || positions.get(0) == null) {
            return Optional.empty();
        }

        Point point = positions.get(0);
        String name = getStoreName(storeId);

        return Optional.of(new StoreLocation(storeId, name, point.getX(), point.getY()));
    }

    @Override
    public Optional<Double> getDistance(String storeId1, String storeId2, String unit) {
        Metrics metric = toMetrics(unit);
        Distance distance = redisTemplate.opsForGeo().distance(GEO_KEY, storeId1, storeId2, metric);

        if (distance == null) {
            return Optional.empty();
        }
        return Optional.of(distance.getValue());
    }

    @Override
    public List<GeoSearchResult> searchNearby(double longitude, double latitude,
                                               double radius, String unit, int count) {
        Metrics metric = toMetrics(unit);
        Distance distance = new Distance(radius, metric);

        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs
                .newGeoSearchArgs()
                .includeDistance()
                .limit(count);

        GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo().search(
                GEO_KEY,
                GeoReference.fromCoordinate(longitude, latitude),
                distance,
                args);

        return toGeoSearchResults(results, unit);
    }

    @Override
    public List<GeoSearchResult> searchNearbyByMember(String storeId, double radius,
                                                       String unit, int count) {
        Metrics metric = toMetrics(unit);
        Distance distance = new Distance(radius, metric);

        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs
                .newGeoSearchArgs()
                .includeDistance()
                .limit(count);

        GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo().search(
                GEO_KEY,
                GeoReference.fromMember(storeId),
                distance,
                args);

        return toGeoSearchResults(results, unit);
    }

    private List<GeoSearchResult> toGeoSearchResults(
            GeoResults<RedisGeoCommands.GeoLocation<Object>> results, String unit) {
        if (results == null) {
            return Collections.emptyList();
        }

        List<GeoSearchResult> searchResults = new ArrayList<>();
        for (var geoResult : results) {
            String memberId = geoResult.getContent().getName().toString();
            double dist = geoResult.getDistance().getValue();
            String name = getStoreName(memberId);

            Optional<StoreLocation> position = getPosition(memberId);
            if (position.isPresent()) {
                StoreLocation location = position.get();
                searchResults.add(new GeoSearchResult(location, dist, unit));
            } else {
                StoreLocation location = new StoreLocation(memberId, name, 0, 0);
                searchResults.add(new GeoSearchResult(location, dist, unit));
            }
        }
        return searchResults;
    }

    private String getStoreName(String storeId) {
        String metaKey = buildMetaKey(storeId);
        Object name = redisTemplate.opsForHash().get(metaKey, "name");
        return name != null ? name.toString() : storeId;
    }

    private String buildMetaKey(String storeId) {
        return RedisKeyConvention.buildKey(SERVICE, ENTITY, "meta") + ":" + storeId;
    }

    private Metrics toMetrics(String unit) {
        return switch (unit.toLowerCase()) {
            case "km" -> Metrics.KILOMETERS;
            case "mi" -> Metrics.MILES;
            case "m" -> Metrics.METERS;
            default -> Metrics.KILOMETERS;
        };
    }
}
