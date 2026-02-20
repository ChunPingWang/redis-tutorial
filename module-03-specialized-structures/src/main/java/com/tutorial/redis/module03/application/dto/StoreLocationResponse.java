package com.tutorial.redis.module03.application.dto;

import com.tutorial.redis.module03.domain.model.StoreLocation;

/**
 * Response DTO for a store location.
 */
public record StoreLocationResponse(
        String storeId,
        String name,
        double longitude,
        double latitude
) {
    public static StoreLocationResponse from(StoreLocation location) {
        return new StoreLocationResponse(
                location.getStoreId(),
                location.getName(),
                location.getLongitude(),
                location.getLatitude()
        );
    }
}
