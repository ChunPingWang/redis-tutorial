package com.tutorial.redis.module01.application.dto;

import com.tutorial.redis.module01.domain.model.Account;
import java.math.BigDecimal;

public record AccountResponse(
        String accountId,
        String holderName,
        BigDecimal balance,
        String currency
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getHolderName(),
                account.getBalance(),
                account.getCurrency()
        );
    }
}
