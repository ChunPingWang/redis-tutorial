package com.tutorial.redis.module03.domain.service;

/**
 * Domain service for geospatial business rules.
 * Pure domain logic — zero framework dependency.
 */
public class GeoService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Validates whether the given coordinates are within valid ranges.
     *
     * @param longitude must be between -180 and 180
     * @param latitude  must be between -90 and 90
     * @return true if both coordinates are valid
     */
    public boolean isValidCoordinate(double longitude, double latitude) {
        return longitude >= -180 && longitude <= 180
                && latitude >= -90 && latitude <= 90;
    }

    /**
     * Estimates the great-circle distance between two points using the Haversine formula.
     * This is an approximation suitable for validation purposes only;
     * use Redis GEODIST for accurate distances.
     *
     * @param lon1 longitude of the first point
     * @param lat1 latitude of the first point
     * @param lon2 longitude of the second point
     * @param lat2 latitude of the second point
     * @return approximate distance in kilometers
     */
    public double estimateDistance(double lon1, double lat1, double lon2, double lat2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
