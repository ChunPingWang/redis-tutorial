package com.tutorial.redis.module14.finance.application.usecase;

import com.tutorial.redis.module14.finance.domain.model.RiskAlert;
import com.tutorial.redis.module14.finance.domain.port.inbound.RiskAlertUseCase;
import com.tutorial.redis.module14.finance.domain.port.outbound.FraudDetectionPort;
import com.tutorial.redis.module14.finance.domain.port.outbound.RiskAlertStreamPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service implementing risk alert use cases.
 *
 * <p>Coordinates between the {@link FraudDetectionPort} (Bloom filter)
 * for deduplication and the {@link RiskAlertStreamPort} (Redis Stream)
 * for alert publishing and consumption.</p>
 */
@Service
public class RiskAlertService implements RiskAlertUseCase {

    private static final Logger log = LoggerFactory.getLogger(RiskAlertService.class);

    private final FraudDetectionPort fraudDetectionPort;
    private final RiskAlertStreamPort riskAlertStreamPort;

    public RiskAlertService(FraudDetectionPort fraudDetectionPort,
                            RiskAlertStreamPort riskAlertStreamPort) {
        this.fraudDetectionPort = fraudDetectionPort;
        this.riskAlertStreamPort = riskAlertStreamPort;
    }

    @Override
    public void publishRiskAlert(RiskAlert alert) {
        log.info("Publishing risk alert {} for account {}", alert.getAlertId(), alert.getAccountId());
        riskAlertStreamPort.publishAlert(alert);
    }

    @Override
    public boolean checkFraudDuplicate(String transactionId) {
        log.info("Checking fraud duplicate for transaction {}", transactionId);
        return fraudDetectionPort.mightExist(transactionId);
    }
}
