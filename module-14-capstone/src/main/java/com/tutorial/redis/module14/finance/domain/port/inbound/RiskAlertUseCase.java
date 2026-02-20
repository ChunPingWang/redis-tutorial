package com.tutorial.redis.module14.finance.domain.port.inbound;

import com.tutorial.redis.module14.finance.domain.model.RiskAlert;

/**
 * Inbound port for risk alert operations.
 *
 * <p>Defines use cases for publishing risk alerts to a Redis Stream
 * and checking for duplicate fraud transactions via a Bloom filter.</p>
 */
public interface RiskAlertUseCase {

    /**
     * Publishes a risk alert to the Redis Stream for downstream consumers.
     *
     * @param alert the risk alert to publish
     */
    void publishRiskAlert(RiskAlert alert);

    /**
     * Checks whether a transaction has already been flagged for fraud
     * using a Bloom filter.
     *
     * @param transactionId the transaction identifier to check
     * @return {@code true} if the transaction might exist (possible false positive),
     *         {@code false} if it definitely does not exist
     */
    boolean checkFraudDuplicate(String transactionId);
}
