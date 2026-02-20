package com.tutorial.redis.module03.domain.model;

import java.util.Objects;

/**
 * Represents a geospatial search result pairing a location with its distance.
 * Returned by GEOSEARCH operations.
 * Immutable value object — all fields are final.
 */
public class GeoSearchResult {

    private final StoreLocation storeLocation;
    private final double distance;
    private final String unit;

    public GeoSearchResult(StoreLocation storeLocation, double distance, String unit) {
        this.storeLocation = Objects.requireNonNull(storeLocation, "storeLocation must not be null");
        this.unit = Objects.requireNonNull(unit, "unit must not be null");
        this.distance = distance;
    }

    public StoreLocation getStoreLocation() { return storeLocation; }
    public double getDistance() { return distance; }
    public String getUnit() { return unit; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeoSearchResult that)) return false;
        return Double.compare(that.distance, distance) == 0
                && storeLocation.equals(that.storeLocation)
                && unit.equals(that.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeLocation, distance, unit);
    }

    @Override
    public String toString() {
        return "GeoSearchResult{storeLocation=%s, distance=%.2f %s}".formatted(
                storeLocation, distance, unit);
    }
}
