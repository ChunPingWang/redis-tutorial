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

/**
 * 帳戶快取 Adapter 整合測試
 * 驗證 RedisAccountCacheAdapter 透過 RedisTemplate 對 Redis 進行 CRUD 操作，
 * 涵蓋 SET/GET/DEL 指令、TTL 過期機制與 Key 命名慣例。
 * 層級：Adapter（外部端口實作）
 */
@DisplayName("RedisAccountCacheAdapter 整合測試")
class RedisAccountCacheAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private AccountCachePort accountCachePort;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Account createTestAccount(String id) {
        return new Account(id, "Test Holder", new BigDecimal("1000.00"), "USD", Instant.now());
    }

    // 驗證儲存帳戶後能成功從 Redis 讀取
    @Test
    @DisplayName("save_WhenValidAccount_StoresInRedis")
    void save_WhenValidAccount_StoresInRedis() {
        Account account = createTestAccount("ACC-001");

        accountCachePort.save(account, Duration.ofMinutes(10));

        assertThat(accountCachePort.findById("ACC-001")).isPresent();
    }

    // 驗證以 ID 查詢已快取帳戶時，回傳完整帳戶資料
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

    // 驗證查詢不存在的帳戶時回傳空值
    @Test
    @DisplayName("findById_WhenAccountNotExists_ReturnsEmpty")
    void findById_WhenAccountNotExists_ReturnsEmpty() {
        Optional<Account> found = accountCachePort.findById("NON-EXISTENT");

        assertThat(found).isEmpty();
    }

    // 驗證刪除已快取帳戶後，該筆資料從 Redis 中移除（DEL 指令）
    @Test
    @DisplayName("evict_WhenAccountExists_RemovesFromRedis")
    void evict_WhenAccountExists_RemovesFromRedis() {
        Account account = createTestAccount("ACC-003");
        accountCachePort.save(account, Duration.ofMinutes(10));

        accountCachePort.evict("ACC-003");

        assertThat(accountCachePort.findById("ACC-003")).isEmpty();
    }

    // 驗證帳戶已快取時 exists 回傳 true
    @Test
    @DisplayName("exists_WhenAccountCached_ReturnsTrue")
    void exists_WhenAccountCached_ReturnsTrue() {
        Account account = createTestAccount("ACC-004");
        accountCachePort.save(account, Duration.ofMinutes(10));

        assertThat(accountCachePort.exists("ACC-004")).isTrue();
    }

    // 驗證帳戶未快取時 exists 回傳 false
    @Test
    @DisplayName("exists_WhenAccountNotCached_ReturnsFalse")
    void exists_WhenAccountNotCached_ReturnsFalse() {
        assertThat(accountCachePort.exists("NON-EXISTENT")).isFalse();
    }

    // 驗證 TTL 過期後帳戶自動從 Redis 移除
    @Test
    @DisplayName("save_WhenTTLExpires_AccountIsEvicted")
    void save_WhenTTLExpires_AccountIsEvicted() throws InterruptedException {
        Account account = createTestAccount("ACC-005");
        accountCachePort.save(account, Duration.ofSeconds(1));

        assertThat(accountCachePort.exists("ACC-005")).isTrue();

        Thread.sleep(1500);

        assertThat(accountCachePort.exists("ACC-005")).isFalse();
    }

    // 驗證 Redis Key 遵循「banking:account:{id}」命名慣例
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
