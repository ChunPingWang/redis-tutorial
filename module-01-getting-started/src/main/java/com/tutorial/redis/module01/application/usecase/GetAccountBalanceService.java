package com.tutorial.redis.module01.application.usecase;

import com.tutorial.redis.module01.domain.model.Account;
import com.tutorial.redis.module01.domain.port.inbound.GetAccountBalanceUseCase;
import com.tutorial.redis.module01.domain.port.outbound.AccountCachePort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetAccountBalanceService implements GetAccountBalanceUseCase {

    private final AccountCachePort accountCachePort;

    public GetAccountBalanceService(AccountCachePort accountCachePort) {
        this.accountCachePort = accountCachePort;
    }

    @Override
    public Optional<Account> getAccount(String accountId) {
        return accountCachePort.findById(accountId);
    }
}
