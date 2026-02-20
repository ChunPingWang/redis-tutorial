package com.tutorial.redis.module05.domain.port.inbound;

import com.tutorial.redis.module05.domain.model.TransferResult;

/**
 * Inbound port: atomic money transfer between two accounts
 * using Redis MULTI/EXEC transactions.
 */
public interface TransferMoneyUseCase {

    /**
     * Transfers an amount from one account to another atomically.
     * Both the debit and credit are executed inside a Redis MULTI/EXEC block
     * to guarantee all-or-nothing semantics.
     *
     * @param fromAccountId the source account to debit
     * @param toAccountId   the target account to credit
     * @param amount        the amount to transfer (must be positive)
     * @return the result of the transfer operation
     */
    TransferResult transfer(String fromAccountId, String toAccountId, double amount);
}
