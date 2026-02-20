package com.tutorial.redis.module14.ecommerce.domain.port.outbound;

import java.util.List;

/**
 * Outbound port for geospatial operations.
 *
 * <p>Abstracts Redis GEO commands for adding locations and finding
 * nearby members within a given radius.</p>
 */
public interface GeoPort {

    void addLocation(String key, double longitude, double latitude, String member);

    List<String> findNearby(String key, double longitude, double latitude, double radiusKm);
}
