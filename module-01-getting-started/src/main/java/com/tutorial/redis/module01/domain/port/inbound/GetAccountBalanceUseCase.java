package com.tutorial.redis.module01.domain.port.inbound;

import com.tutorial.redis.module01.domain.model.Account;
import java.util.Optional;

/**
 * Inbound port: retrieve account balance (cache-first strategy).
 */
public interface GetAccountBalanceUseCase {

    Optional<Account> getAccount(String accountId);
}
