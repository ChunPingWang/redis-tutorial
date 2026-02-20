package com.tutorial.redis.module03.adapter.inbound.rest;

import com.tutorial.redis.module03.application.dto.NearbyStoreResponse;
import com.tutorial.redis.module03.application.dto.StoreLocationResponse;
import com.tutorial.redis.module03.domain.model.StoreLocation;
import com.tutorial.redis.module03.domain.port.inbound.SearchNearbyUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for geospatial store operations.
 *
 * <p>Demonstrates Redis Geospatial operations (GEOADD, GEODIST, GEOSEARCH)
 * through location registration and nearby store search endpoints.</p>
 */
@RestController
@RequestMapping("/api/v1/geo/stores")
public class GeoController {

    private final SearchNearbyUseCase searchNearbyUseCase;

    public GeoController(SearchNearbyUseCase searchNearbyUseCase) {
        this.searchNearbyUseCase = searchNearbyUseCase;
    }

    @PostMapping
    public ResponseEntity<StoreLocationResponse> registerStore(@RequestBody Map<String, Object> body) {
        String storeId = (String) body.get("storeId");
        String name = (String) body.get("name");
        double longitude = ((Number) body.get("longitude")).doubleValue();
        double latitude = ((Number) body.get("latitude")).doubleValue();

        StoreLocation location = new StoreLocation(storeId, name, longitude, latitude);
        searchNearbyUseCase.registerStore(location);
        return ResponseEntity.ok(StoreLocationResponse.from(location));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyStoreResponse>> findNearbyStores(
            @RequestParam double lon,
            @RequestParam double lat,
            @RequestParam double radiusKm,
            @RequestParam(defaultValue = "10") int maxResults) {
        List<NearbyStoreResponse> results = searchNearbyUseCase
                .findNearbyStores(lon, lat, radiusKm, maxResults)
                .stream()
                .map(NearbyStoreResponse::from)
                .toList();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{storeId1}/distance/{storeId2}")
    public ResponseEntity<Map<String, Object>> getDistanceBetweenStores(
            @PathVariable String storeId1,
            @PathVariable String storeId2) {
        return searchNearbyUseCase.getDistanceBetweenStores(storeId1, storeId2)
                .map(distance -> ResponseEntity.ok(Map.<String, Object>of(
                        "storeId1", storeId1,
                        "storeId2", storeId2,
                        "distanceKm", distance,
                        "unit", "km")))
                .orElse(ResponseEntity.notFound().build());
    }
}
