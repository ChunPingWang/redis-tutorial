package com.tutorial.redis.module03.domain.port.inbound;

import com.tutorial.redis.module03.domain.model.GeoSearchResult;
import com.tutorial.redis.module03.domain.model.StoreLocation;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: search for nearby stores using Redis Geospatial structure.
 */
public interface SearchNearbyUseCase {

    /**
     * Registers a store location for geo search.
     */
    void registerStore(StoreLocation location);

    /**
     * Finds stores near a given coordinate within the specified radius.
     *
     * @param longitude  center longitude
     * @param latitude   center latitude
     * @param radiusKm   search radius in kilometers
     * @param maxResults maximum number of results to return
     * @return list of nearby stores with distances, ordered by distance ascending
     */
    List<GeoSearchResult> findNearbyStores(double longitude, double latitude, double radiusKm, int maxResults);

    /**
     * Calculates the distance between two registered stores in kilometers.
     *
     * @return the distance in km, or empty if either store is not found
     */
    Optional<Double> getDistanceBetweenStores(String storeId1, String storeId2);
}
