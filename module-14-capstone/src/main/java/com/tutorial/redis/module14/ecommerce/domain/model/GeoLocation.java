package com.tutorial.redis.module14.ecommerce.domain.model;

/**
 * Store location value object.
 *
 * <p>Represents a physical store with geographic coordinates (longitude
 * and latitude). Used with Redis GEO commands for nearby store lookups.</p>
 */
public class GeoLocation {

    private String storeId;
    private String name;
    private double longitude;
    private double latitude;

    public GeoLocation() {
    }

    public GeoLocation(String storeId, String name, double longitude, double latitude) {
        this.storeId = storeId;
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public String toString() {
        return "GeoLocation{storeId='" + storeId + "', name='" + name
                + "', longitude=" + longitude + ", latitude=" + latitude + '}';
    }
}
