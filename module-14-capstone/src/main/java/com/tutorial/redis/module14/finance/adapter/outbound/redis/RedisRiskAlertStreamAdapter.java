package com.tutorial.redis.module14.finance.adapter.outbound.redis;

import com.tutorial.redis.module14.finance.domain.model.RiskAlert;
import com.tutorial.redis.module14.finance.domain.port.outbound.RiskAlertStreamPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis adapter for risk alert stream operations.
 *
 * <p>Implements {@link RiskAlertStreamPort} using Redis Streams for
 * publishing and consuming risk alerts. Supports consumer groups for
 * reliable, distributed alert processing.</p>
 */
@Component
public class RedisRiskAlertStreamAdapter implements RiskAlertStreamPort {

    private static final Logger log = LoggerFactory.getLogger(RedisRiskAlertStreamAdapter.class);

    private static final String STREAM_KEY = "finance:risk:alerts";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisRiskAlertStreamAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void publishAlert(RiskAlert alert) {
        Map<String, String> map = new HashMap<>();
        map.put("alertId", alert.getAlertId());
        map.put("accountId", alert.getAccountId());
        map.put("alertType", alert.getAlertType());
        map.put("description", alert.getDescription());
        map.put("severity", alert.getSeverity());
        map.put("timestamp", String.valueOf(alert.getTimestamp()));

        stringRedisTemplate.opsForStream().add(STREAM_KEY, map);
        log.debug("Published risk alert {} to stream", alert.getAlertId());
    }

    @Override
    public List<RiskAlert> consumeAlerts(String groupName, String consumerName, int count) {
        ensureConsumerGroupExists(groupName);

        try {
            @SuppressWarnings("unchecked")
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                    Consumer.from(groupName, consumerName),
                    StreamReadOptions.empty().count(count),
                    StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

            if (records == null || records.isEmpty()) {
                return Collections.emptyList();
            }

            List<RiskAlert> alerts = new ArrayList<>();
            for (MapRecord<String, Object, Object> record : records) {
                Map<Object, Object> value = record.getValue();
                RiskAlert alert = new RiskAlert();
                alert.setAlertId(String.valueOf(value.getOrDefault("alertId", "")));
                alert.setAccountId(String.valueOf(value.getOrDefault("accountId", "")));
                alert.setAlertType(String.valueOf(value.getOrDefault("alertType", "")));
                alert.setDescription(String.valueOf(value.getOrDefault("description", "")));
                alert.setSeverity(String.valueOf(value.getOrDefault("severity", "")));
                String ts = String.valueOf(value.getOrDefault("timestamp", "0"));
                alert.setTimestamp(Long.parseLong(ts));
                alerts.add(alert);

                // Acknowledge the message
                stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, groupName, record.getId());
            }

            log.debug("Consumed {} alerts from stream", alerts.size());
            return alerts;
        } catch (Exception e) {
            log.warn("Failed to consume alerts: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Ensures the consumer group exists, creating it if necessary.
     * Silently ignores the error if the group already exists.
     */
    private void ensureConsumerGroupExists(String groupName) {
        try {
            stringRedisTemplate.opsForStream()
                    .createGroup(STREAM_KEY, ReadOffset.from("0"), groupName);
            log.debug("Created consumer group '{}'", groupName);
        } catch (Exception e) {
            // Group already exists or stream doesn't exist yet — this is expected
            log.trace("Consumer group creation returned: {}", e.getMessage());
        }
    }
}
