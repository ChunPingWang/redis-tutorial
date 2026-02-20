package com.tutorial.redis.module05.domain.port.outbound;

import com.tutorial.redis.module05.domain.model.TransferResult;

/**
 * Outbound port for Redis MULTI/EXEC transaction operations.
 * Ensures atomic execution of a debit from one account and credit to another.
 * Implemented by a Redis adapter in the infrastructure layer.
 */
public interface TransactionPort {

    /**
     * Atomically transfers an amount from one account to another using
     * Redis MULTI/EXEC to ensure both the debit and credit execute together.
     *
     * @param fromAccountId the source account to debit
     * @param toAccountId   the target account to credit
     * @param amount        the amount to transfer (must be positive)
     * @return the result of the transfer operation
     */
    TransferResult transfer(String fromAccountId, String toAccountId, double amount);
}
