package com.tutorial.redis.module03.domain.port.outbound;

import com.tutorial.redis.module03.domain.model.GeoSearchResult;
import com.tutorial.redis.module03.domain.model.StoreLocation;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for geospatial location operations.
 * Uses Redis Geospatial structure (GEOADD / GEOPOS / GEODIST / GEOSEARCH).
 * Implemented by Redis adapter in the infrastructure layer.
 */
public interface GeoLocationPort {

    /**
     * Adds a single store location (GEOADD).
     */
    void addLocation(StoreLocation location);

    /**
     * Adds multiple store locations in bulk (GEOADD).
     */
    void addLocations(List<StoreLocation> locations);

    /**
     * Retrieves the position of a store by its ID (GEOPOS).
     *
     * @return the store location with coordinates, or empty if not found
     */
    Optional<StoreLocation> getPosition(String storeId);

    /**
     * Calculates the distance between two stores (GEODIST).
     *
     * @param unit distance unit: "km", "mi", or "m"
     * @return the distance, or empty if either store is not found
     */
    Optional<Double> getDistance(String storeId1, String storeId2, String unit);

    /**
     * Searches for stores near a given coordinate (GEOSEARCH).
     *
     * @param longitude center longitude
     * @param latitude  center latitude
     * @param radius    search radius
     * @param unit      distance unit: "km", "mi", or "m"
     * @param count     maximum number of results
     * @return list of matching stores with distances, ordered by distance ascending
     */
    List<GeoSearchResult> searchNearby(double longitude, double latitude, double radius, String unit, int count);

    /**
     * Searches for stores near an existing member (GEOSEARCH FROMMEMBER).
     *
     * @param storeId the member store ID to search from
     * @param radius  search radius
     * @param unit    distance unit: "km", "mi", or "m"
     * @param count   maximum number of results
     * @return list of matching stores with distances, ordered by distance ascending
     */
    List<GeoSearchResult> searchNearbyByMember(String storeId, double radius, String unit, int count);
}
