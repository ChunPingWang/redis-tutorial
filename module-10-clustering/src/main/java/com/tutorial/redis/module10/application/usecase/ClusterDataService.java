package com.tutorial.redis.module10.application.usecase;

import com.tutorial.redis.module10.domain.port.inbound.ClusterDataUseCase;
import com.tutorial.redis.module10.domain.port.outbound.ClusterDataPort;
import com.tutorial.redis.module10.domain.service.HashSlotCalculator;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application service implementing {@link ClusterDataUseCase}.
 *
 * <p>Provides data read/write operations with cluster-aware hash tag support.
 * When using hash tags, keys are formatted as {@code {hashTag}:subKey} to
 * ensure all related keys map to the same hash slot in a Redis Cluster
 * deployment.</p>
 *
 * <p>Delegates actual Redis I/O to the {@link ClusterDataPort} outbound port
 * and uses the {@link HashSlotCalculator} for slot-aware key construction.</p>
 */
@Service
public class ClusterDataService implements ClusterDataUseCase {

    private final ClusterDataPort clusterDataPort;
    private final HashSlotCalculator hashSlotCalculator;

    public ClusterDataService(ClusterDataPort clusterDataPort,
                              HashSlotCalculator hashSlotCalculator) {
        this.clusterDataPort = clusterDataPort;
        this.hashSlotCalculator = hashSlotCalculator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeData(String key, String value) {
        clusterDataPort.writeData(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readData(String key) {
        return clusterDataPort.readData(key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds keys in the format {@code {hashTag}:subKey} for each entry
     * in the provided map, then writes all key-value pairs via the outbound
     * port. The hash tag ensures all keys are co-located in the same slot.</p>
     */
    @Override
    public void writeWithHashTag(String hashTag, Map<String, String> subKeyValues) {
        Map<String, String> taggedKeyValues = new LinkedHashMap<>();
        subKeyValues.forEach((subKey, value) ->
                taggedKeyValues.put(buildTaggedKey(hashTag, subKey), value));
        clusterDataPort.writeMultipleKeys(taggedKeyValues);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds keys in the format {@code {hashTag}:subKey} for each sub-key,
     * reads all values via the outbound port, then maps the results back to
     * the original sub-keys.</p>
     */
    @Override
    public Map<String, String> readWithHashTag(String hashTag, List<String> subKeys) {
        List<String> taggedKeys = subKeys.stream()
                .map(subKey -> buildTaggedKey(hashTag, subKey))
                .collect(Collectors.toList());

        Map<String, String> taggedResult = clusterDataPort.readMultipleKeys(taggedKeys);

        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < subKeys.size(); i++) {
            result.put(subKeys.get(i), taggedResult.get(taggedKeys.get(i)));
        }
        return result;
    }

    /**
     * Builds a Redis key with a hash tag in the format {@code {hashTag}:subKey}.
     *
     * @param hashTag the hash tag for slot co-location
     * @param subKey  the sub-key identifying the specific data item
     * @return the formatted key with hash tag
     */
    private String buildTaggedKey(String hashTag, String subKey) {
        return "{%s}:%s".formatted(hashTag, subKey);
    }
}
