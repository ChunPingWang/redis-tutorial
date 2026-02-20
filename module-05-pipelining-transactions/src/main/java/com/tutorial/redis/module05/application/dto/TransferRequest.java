package com.tutorial.redis.module05.application.dto;

/**
 * Request DTO for account-to-account money transfers.
 *
 * @param fromAccountId the source account to debit
 * @param toAccountId   the target account to credit
 * @param amount        the amount to transfer (must be positive)
 */
public record TransferRequest(
        String fromAccountId,
        String toAccountId,
        double amount
) {
}
