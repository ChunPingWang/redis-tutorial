package com.tutorial.redis.module01.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module01.domain.model.Account;
import com.tutorial.redis.module01.domain.port.outbound.AccountCachePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisAccountCacheAdapter 整合測試")
class RedisAccountCacheAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private AccountCachePort accountCachePort;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Account createTestAccount(String id) {
        return new Account(id, "Test Holder", new BigDecimal("1000.00"), "USD", Instant.now());
    }

    @Test
    @DisplayName("save_WhenValidAccount_StoresInRedis")
    void save_WhenValidAccount_StoresInRedis() {
        Account account = createTestAccount("ACC-001");

        accountCachePort.save(account, Duration.ofMinutes(10));

        assertThat(accountCachePort.findById("ACC-001")).isPresent();
    }

    @Test
    @DisplayName("findById_WhenAccountExists_ReturnsAccount")
    void findById_WhenAccountExists_ReturnsAccount() {
        Account account = createTestAccount("ACC-002");
        accountCachePort.save(account, Duration.ofMinutes(10));

        Optional<Account> found = accountCachePort.findById("ACC-002");

        assertThat(found).isPresent();
        assertThat(found.get().getAccountId()).isEqualTo("ACC-002");
        assertThat(found.get().getHolderName()).isEqualTo("Test Holder");
        assertThat(found.get().getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(found.get().getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("findById_WhenAccountNotExists_ReturnsEmpty")
    void findById_WhenAccountNotExists_ReturnsEmpty() {
        Optional<Account> found = accountCachePort.findById("NON-EXISTENT");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("evict_WhenAccountExists_RemovesFromRedis")
    void evict_WhenAccountExists_RemovesFromRedis() {
        Account account = createTestAccount("ACC-003");
        accountCachePort.save(account, Duration.ofMinutes(10));

        accountCachePort.evict("ACC-003");

        assertThat(accountCachePort.findById("ACC-003")).isEmpty();
    }

    @Test
    @DisplayName("exists_WhenAccountCached_ReturnsTrue")
    void exists_WhenAccountCached_ReturnsTrue() {
        Account account = createTestAccount("ACC-004");
        accountCachePort.save(account, Duration.ofMinutes(10));

        assertThat(accountCachePort.exists("ACC-004")).isTrue();
    }

    @Test
    @DisplayName("exists_WhenAccountNotCached_ReturnsFalse")
    void exists_WhenAccountNotCached_ReturnsFalse() {
        assertThat(accountCachePort.exists("NON-EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("save_WhenTTLExpires_AccountIsEvicted")
    void save_WhenTTLExpires_AccountIsEvicted() throws InterruptedException {
        Account account = createTestAccount("ACC-005");
        accountCachePort.save(account, Duration.ofSeconds(1));

        assertThat(accountCachePort.exists("ACC-005")).isTrue();

        Thread.sleep(1500);

        assertThat(accountCachePort.exists("ACC-005")).isFalse();
    }

    @Test
    @DisplayName("save_KeyFollowsNamingConvention")
    void save_KeyFollowsNamingConvention() {
        Account account = createTestAccount("ACC-006");
        accountCachePort.save(account, Duration.ofMinutes(10));

        Set<String> keys = stringRedisTemplate.keys("banking:account:ACC-006");
        assertThat(keys).isNotNull().hasSize(1);
        assertThat(keys.iterator().next()).matches("banking:account:ACC-006");
    }
}
