package com.tutorial.redis.module01.domain.service;

import com.tutorial.redis.module01.domain.model.Account;

import java.math.BigDecimal;

/**
 * Domain service for account business rules.
 * Pure domain logic — zero framework dependency.
 */
public class AccountService {

    /**
     * Validates that an account balance is non-negative.
     */
    public boolean isBalanceValid(Account account) {
        return account.getBalance().compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Checks if the account has sufficient balance for a withdrawal.
     */
    public boolean hasSufficientBalance(Account account, BigDecimal amount) {
        return account.getBalance().compareTo(amount) >= 0;
    }
}
