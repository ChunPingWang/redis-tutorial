package com.tutorial.redis.module03.application.usecase;

import com.tutorial.redis.module03.domain.model.GeoSearchResult;
import com.tutorial.redis.module03.domain.model.StoreLocation;
import com.tutorial.redis.module03.domain.port.inbound.SearchNearbyUseCase;
import com.tutorial.redis.module03.domain.port.outbound.GeoLocationPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementing nearby store search use cases.
 *
 * <p>Delegates to {@link GeoLocationPort} for Redis Geospatial operations.
 * Demonstrates GEOADD, GEODIST, and GEOSEARCH for location-based queries.</p>
 */
@Service
public class SearchNearbyService implements SearchNearbyUseCase {

    private final GeoLocationPort geoLocationPort;

    public SearchNearbyService(GeoLocationPort geoLocationPort) {
        this.geoLocationPort = geoLocationPort;
    }

    @Override
    public void registerStore(StoreLocation location) {
        geoLocationPort.addLocation(location);
    }

    @Override
    public List<GeoSearchResult> findNearbyStores(double longitude, double latitude,
                                                   double radiusKm, int maxResults) {
        return geoLocationPort.searchNearby(longitude, latitude, radiusKm, "km", maxResults);
    }

    @Override
    public Optional<Double> getDistanceBetweenStores(String storeId1, String storeId2) {
        return geoLocationPort.getDistance(storeId1, storeId2, "km");
    }
}
