package com.tutorial.redis.module14.ecommerce.domain.port.inbound;

import com.tutorial.redis.module14.ecommerce.domain.model.GeoLocation;

import java.util.List;

/**
 * Inbound port for store locator operations.
 *
 * <p>Defines use cases for adding store locations and finding stores
 * within a given radius using Redis GEO commands.</p>
 */
public interface StoreLocatorUseCase {

    void addStore(GeoLocation store);

    List<String> findNearbyStores(double longitude, double latitude, double radiusKm);
}
