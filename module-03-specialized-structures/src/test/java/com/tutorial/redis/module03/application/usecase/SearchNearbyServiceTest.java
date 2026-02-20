package com.tutorial.redis.module03.application.usecase;

import com.tutorial.redis.module03.domain.model.GeoSearchResult;
import com.tutorial.redis.module03.domain.model.StoreLocation;
import com.tutorial.redis.module03.domain.port.outbound.GeoLocationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchNearbyService 單元測試")
class SearchNearbyServiceTest {

    @Mock
    private GeoLocationPort geoLocationPort;

    @InjectMocks
    private SearchNearbyService service;

    @Test
    @DisplayName("registerStore_DelegatesToPort — 委派至 Port 的 addLocation 方法")
    void registerStore_DelegatesToPort() {
        StoreLocation location = new StoreLocation("STORE-001", "Taipei 101", 121.5654, 25.0330);

        service.registerStore(location);

        verify(geoLocationPort).addLocation(location);
    }

    @Test
    @DisplayName("findNearbyStores_DelegatesToPort — 委派至 Port 的 searchNearby 方法")
    void findNearbyStores_DelegatesToPort() {
        StoreLocation store = new StoreLocation("STORE-001", "Taipei 101", 121.5654, 25.0330);
        GeoSearchResult result = new GeoSearchResult(store, 1.5, "km");
        when(geoLocationPort.searchNearby(121.54, 25.04, 10, "km", 10))
                .thenReturn(List.of(result));

        List<GeoSearchResult> results = service.findNearbyStores(121.54, 25.04, 10, 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStoreLocation().getStoreId()).isEqualTo("STORE-001");
        verify(geoLocationPort).searchNearby(121.54, 25.04, 10, "km", 10);
    }

    @Test
    @DisplayName("getDistanceBetweenStores_DelegatesToPort — 委派至 Port 的 getDistance 方法")
    void getDistanceBetweenStores_DelegatesToPort() {
        when(geoLocationPort.getDistance("STORE-001", "STORE-002", "km"))
                .thenReturn(Optional.of(5.2));

        Optional<Double> distance = service.getDistanceBetweenStores("STORE-001", "STORE-002");

        assertThat(distance).isPresent().contains(5.2);
        verify(geoLocationPort).getDistance("STORE-001", "STORE-002", "km");
    }
}
