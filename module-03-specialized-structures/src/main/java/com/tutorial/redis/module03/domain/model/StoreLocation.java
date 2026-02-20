package com.tutorial.redis.module03.domain.model;

import java.util.Objects;

/**
 * Represents a store location with geospatial coordinates.
 * Maps to Redis Geospatial structure (GEOADD / GEOPOS / GEOSEARCH).
 * Immutable value object — all fields are final.
 */
public class StoreLocation {

    private final String storeId;
    private final String name;
    private final double longitude;
    private final double latitude;

    public StoreLocation(String storeId, String name, double longitude, double latitude) {
        this.storeId = Objects.requireNonNull(storeId, "storeId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180, got: " + longitude);
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90, got: " + latitude);
        }
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getStoreId() { return storeId; }
    public String getName() { return name; }
    public double getLongitude() { return longitude; }
    public double getLatitude() { return latitude; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoreLocation that)) return false;
        return storeId.equals(that.storeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeId);
    }

    @Override
    public String toString() {
        return "StoreLocation{storeId='%s', name='%s', longitude=%.6f, latitude=%.6f}".formatted(
                storeId, name, longitude, latitude);
    }
}
