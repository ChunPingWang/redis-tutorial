package com.tutorial.redis.module06.application.usecase;

import com.tutorial.redis.module06.domain.model.AccountAggregate;
import com.tutorial.redis.module06.domain.port.inbound.ManageAccountUseCase;
import com.tutorial.redis.module06.domain.port.outbound.AccountDaoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service for managing accounts using the DAO pattern with Redis.
 * Delegates all CRUD and index-based query operations to the
 * {@link AccountDaoPort} outbound port.
 */
@Service
public class ManageAccountService implements ManageAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageAccountService.class);

    private final AccountDaoPort accountDaoPort;

    public ManageAccountService(AccountDaoPort accountDaoPort) {
        this.accountDaoPort = accountDaoPort;
    }

    @Override
    public void createAccount(AccountAggregate account) {
        log.debug("Creating account {}", account.getAccountId());
        accountDaoPort.save(account);
    }

    @Override
    public Optional<AccountAggregate> getAccount(String accountId) {
        log.debug("Retrieving account {}", accountId);
        return accountDaoPort.findById(accountId);
    }

    @Override
    public void deleteAccount(String accountId) {
        log.debug("Deleting account {}", accountId);
        accountDaoPort.delete(accountId);
    }

    @Override
    public List<AccountAggregate> findAccountsByCurrency(String currency) {
        log.debug("Finding accounts by currency {}", currency);
        return accountDaoPort.findByCurrency(currency);
    }

    @Override
    public List<AccountAggregate> findAccountsByStatus(String status) {
        log.debug("Finding accounts by status {}", status);
        return accountDaoPort.findByStatus(status);
    }
}
