package com.tutorial.redis.module14.ecommerce.application.usecase;

import com.tutorial.redis.module14.ecommerce.domain.model.GeoLocation;
import com.tutorial.redis.module14.ecommerce.domain.port.inbound.StoreLocatorUseCase;
import com.tutorial.redis.module14.ecommerce.domain.port.outbound.GeoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service implementing store locator use cases.
 *
 * <p>Delegates to the {@link GeoPort} for adding store locations and
 * finding nearby stores using Redis GEO commands.</p>
 */
@Service
public class StoreLocatorService implements StoreLocatorUseCase {

    private static final Logger log = LoggerFactory.getLogger(StoreLocatorService.class);
    private static final String STORES_GEO_KEY = "ecommerce:stores";

    private final GeoPort geoPort;

    public StoreLocatorService(GeoPort geoPort) {
        this.geoPort = geoPort;
    }

    @Override
    public void addStore(GeoLocation store) {
        log.info("Adding store {} at ({}, {})", store.getStoreId(),
                store.getLongitude(), store.getLatitude());
        geoPort.addLocation(STORES_GEO_KEY, store.getLongitude(),
                store.getLatitude(), store.getStoreId());
    }

    @Override
    public List<String> findNearbyStores(double longitude, double latitude, double radiusKm) {
        log.info("Finding stores near ({}, {}) within {} km", longitude, latitude, radiusKm);
        return geoPort.findNearby(STORES_GEO_KEY, longitude, latitude, radiusKm);
    }
}
