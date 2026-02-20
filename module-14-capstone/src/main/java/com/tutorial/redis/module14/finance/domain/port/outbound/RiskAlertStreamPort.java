package com.tutorial.redis.module14.finance.domain.port.outbound;

import com.tutorial.redis.module14.finance.domain.model.RiskAlert;

import java.util.List;

/**
 * Outbound port for risk alert stream operations.
 *
 * <p>Abstracts the Redis Stream used to publish and consume risk alerts.
 * Supports consumer groups for reliable, distributed alert processing.</p>
 */
public interface RiskAlertStreamPort {

    /**
     * Publishes a risk alert to the Redis Stream.
     *
     * @param alert the risk alert to publish
     */
    void publishAlert(RiskAlert alert);

    /**
     * Consumes risk alerts from the Redis Stream using a consumer group.
     *
     * @param groupName    the consumer group name
     * @param consumerName the consumer name within the group
     * @param count        the maximum number of alerts to consume
     * @return list of consumed risk alerts
     */
    List<RiskAlert> consumeAlerts(String groupName, String consumerName, int count);
}
