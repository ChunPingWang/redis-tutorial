package com.tutorial.redis.module05.application.usecase;

import com.tutorial.redis.module05.domain.model.TransferResult;
import com.tutorial.redis.module05.domain.port.inbound.TransferMoneyUseCase;
import com.tutorial.redis.module05.domain.port.outbound.TransactionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service for atomic money transfers using Redis MULTI/EXEC transactions.
 * Delegates to the {@link TransactionPort} outbound port which handles
 * WATCH, MULTI, and EXEC for transactional guarantees.
 */
@Service
public class TransferMoneyService implements TransferMoneyUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferMoneyService.class);

    private final TransactionPort transactionPort;

    public TransferMoneyService(TransactionPort transactionPort) {
        this.transactionPort = transactionPort;
    }

    @Override
    public TransferResult transfer(String fromAccountId, String toAccountId, double amount) {
        log.debug("Initiating transfer of {} from account {} to account {}",
                amount, fromAccountId, toAccountId);
        return transactionPort.transfer(fromAccountId, toAccountId, amount);
    }
}
