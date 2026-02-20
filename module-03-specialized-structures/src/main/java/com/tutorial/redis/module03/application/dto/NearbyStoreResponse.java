package com.tutorial.redis.module03.application.dto;

import com.tutorial.redis.module03.domain.model.GeoSearchResult;

/**
 * Response DTO for a nearby store search result, including distance.
 */
public record NearbyStoreResponse(
        String storeId,
        String name,
        double longitude,
        double latitude,
        double distanceKm
) {
    public static NearbyStoreResponse from(GeoSearchResult result) {
        return new NearbyStoreResponse(
                result.getStoreLocation().getStoreId(),
                result.getStoreLocation().getName(),
                result.getStoreLocation().getLongitude(),
                result.getStoreLocation().getLatitude(),
                result.getDistance()
        );
    }
}
