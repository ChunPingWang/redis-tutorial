package com.tutorial.redis.module14.finance.application.usecase;

import com.tutorial.redis.module14.finance.domain.model.Account;
import com.tutorial.redis.module14.finance.domain.port.inbound.AccountManagementUseCase;
import com.tutorial.redis.module14.finance.domain.port.outbound.AccountCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service implementing account management use cases.
 *
 * <p>Delegates balance caching to Redis Strings and profile storage to
 * RedisJSON via the {@link AccountCachePort}. Handles JSON serialization
 * and deserialization of {@link Account} objects using simple string
 * concatenation (no Jackson dependency required).</p>
 */
@Service
public class AccountService implements AccountManagementUseCase {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountCachePort accountCachePort;

    public AccountService(AccountCachePort accountCachePort) {
        this.accountCachePort = accountCachePort;
    }

    @Override
    public void cacheAccountBalance(String accountId, double balance) {
        log.info("Caching balance for account {}: {}", accountId, balance);
        accountCachePort.setBalance(accountId, balance);
    }

    @Override
    public Double getAccountBalance(String accountId) {
        log.info("Retrieving balance for account {}", accountId);
        return accountCachePort.getBalance(accountId);
    }

    @Override
    public void storeAccountProfile(Account account) {
        log.info("Storing profile for account {}", account.getAccountId());
        String json = serializeAccount(account);
        accountCachePort.storeProfile(account.getAccountId(), json);
    }

    @Override
    public Account getAccountProfile(String accountId) {
        log.info("Retrieving profile for account {}", accountId);
        String json = accountCachePort.getProfile(accountId);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return deserializeAccount(json);
    }

    /**
     * Serializes an Account to a JSON string using simple concatenation.
     */
    private String serializeAccount(Account account) {
        return "{\"accountId\":\"" + account.getAccountId() + "\""
                + ",\"ownerName\":\"" + account.getOwnerName() + "\""
                + ",\"balance\":" + account.getBalance()
                + ",\"currency\":\"" + account.getCurrency() + "\""
                + ",\"createdAt\":" + account.getCreatedAt() + "}";
    }

    /**
     * Deserializes an Account from a JSON string using simple parsing.
     *
     * <p>Handles both raw JSON objects and JSON arrays returned by
     * {@code JSON.GET} (which wraps results in square brackets when
     * using the {@code $} path).</p>
     */
    private Account deserializeAccount(String json) {
        // JSON.GET with '$' path returns an array: [{"accountId":...}]
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (trimmed.isEmpty()) {
            return null;
        }

        Account account = new Account();
        account.setAccountId(extractStringValue(trimmed, "accountId"));
        account.setOwnerName(extractStringValue(trimmed, "ownerName"));
        account.setBalance(extractDoubleValue(trimmed, "balance"));
        account.setCurrency(extractStringValue(trimmed, "currency"));
        account.setCreatedAt(extractLongValue(trimmed, "createdAt"));
        return account;
    }

    private String extractStringValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start < 0) {
            return null;
        }
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end < 0) {
            return null;
        }
        return json.substring(start, end);
    }

    private double extractDoubleValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start < 0) {
            return 0.0;
        }
        start += searchKey.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.' || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private long extractLongValue(String json, String key) {
        return (long) extractDoubleValue(json, key);
    }
}
