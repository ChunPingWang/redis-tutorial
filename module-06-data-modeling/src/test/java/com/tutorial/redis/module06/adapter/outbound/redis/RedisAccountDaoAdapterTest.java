package com.tutorial.redis.module06.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module06.domain.model.AccountAggregate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisAccountDaoAdapter 整合測試")
class RedisAccountDaoAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisAccountDaoAdapter adapter;

    @Test
    @DisplayName("save_AndFindById_ReturnsAccount — 儲存帳戶後以 ID 查詢，應回傳完整欄位")
    void save_AndFindById_ReturnsAccount() {
        // Arrange
        Instant now = Instant.parse("2024-06-15T10:30:00Z");
        AccountAggregate account = new AccountAggregate(
                "acct-001", "Alice Wang", 5000.50, "USD", now, "ACTIVE"
        );

        // Act
        adapter.save(account);
        Optional<AccountAggregate> found = adapter.findById("acct-001");

        // Assert
        assertThat(found).isPresent();
        AccountAggregate result = found.get();
        assertThat(result.getAccountId()).isEqualTo("acct-001");
        assertThat(result.getHolderName()).isEqualTo("Alice Wang");
        assertThat(result.getBalance()).isEqualTo(5000.50);
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        // Verify Hash fields exist in Redis
        assertThat(stringRedisTemplate.opsForHash().get("banking:account:acct-001", "holderName"))
                .isEqualTo("Alice Wang");
        assertThat(stringRedisTemplate.opsForHash().get("banking:account:acct-001", "currency"))
                .isEqualTo("USD");
    }

    @Test
    @DisplayName("findById_WhenNotExists_ReturnsEmpty — 查詢不存在的帳戶，應回傳空 Optional")
    void findById_WhenNotExists_ReturnsEmpty() {
        // Act
        Optional<AccountAggregate> result = adapter.findById("non-existent-id");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("delete_RemovesAccountAndIndexes — 刪除帳戶後，實體與二級索引都應被清除")
    void delete_RemovesAccountAndIndexes() {
        // Arrange
        AccountAggregate account = new AccountAggregate(
                "acct-del", "Bob Chen", 3000.00, "TWD", Instant.now(), "ACTIVE"
        );
        adapter.save(account);

        // Verify saved
        assertThat(adapter.findById("acct-del")).isPresent();
        assertThat(adapter.findByCurrency("TWD")).hasSize(1);
        assertThat(adapter.findByStatus("ACTIVE")).hasSize(1);

        // Act
        adapter.delete("acct-del");

        // Assert — entity removed
        assertThat(adapter.findById("acct-del")).isEmpty();

        // Assert — indexes cleaned
        assertThat(adapter.findByCurrency("TWD")).isEmpty();
        assertThat(adapter.findByStatus("ACTIVE")).isEmpty();
    }

    @Test
    @DisplayName("findByCurrency_ReturnsMatchingAccounts — 3 筆帳戶 (2 USD, 1 TWD)，查詢 USD 應回傳 2 筆")
    void findByCurrency_ReturnsMatchingAccounts() {
        // Arrange
        adapter.save(new AccountAggregate(
                "acct-usd-1", "Alice", 1000.00, "USD", Instant.now(), "ACTIVE"));
        adapter.save(new AccountAggregate(
                "acct-usd-2", "Bob", 2000.00, "USD", Instant.now(), "ACTIVE"));
        adapter.save(new AccountAggregate(
                "acct-twd-1", "Charlie", 50000.00, "TWD", Instant.now(), "ACTIVE"));

        // Act
        List<AccountAggregate> usdAccounts = adapter.findByCurrency("USD");
        List<AccountAggregate> twdAccounts = adapter.findByCurrency("TWD");

        // Assert
        assertThat(usdAccounts).hasSize(2);
        assertThat(usdAccounts).extracting(AccountAggregate::getAccountId)
                .containsExactlyInAnyOrder("acct-usd-1", "acct-usd-2");

        assertThat(twdAccounts).hasSize(1);
        assertThat(twdAccounts.getFirst().getAccountId()).isEqualTo("acct-twd-1");
    }

    @Test
    @DisplayName("findByStatus_ReturnsMatchingAccounts — 3 筆帳戶 (2 ACTIVE, 1 FROZEN)，查詢 ACTIVE 應回傳 2 筆")
    void findByStatus_ReturnsMatchingAccounts() {
        // Arrange
        adapter.save(new AccountAggregate(
                "acct-a1", "Alice", 1000.00, "USD", Instant.now(), "ACTIVE"));
        adapter.save(new AccountAggregate(
                "acct-a2", "Bob", 2000.00, "TWD", Instant.now(), "ACTIVE"));
        adapter.save(new AccountAggregate(
                "acct-f1", "Charlie", 3000.00, "USD", Instant.now(), "FROZEN"));

        // Act
        List<AccountAggregate> activeAccounts = adapter.findByStatus("ACTIVE");
        List<AccountAggregate> frozenAccounts = adapter.findByStatus("FROZEN");

        // Assert
        assertThat(activeAccounts).hasSize(2);
        assertThat(activeAccounts).extracting(AccountAggregate::getAccountId)
                .containsExactlyInAnyOrder("acct-a1", "acct-a2");

        assertThat(frozenAccounts).hasSize(1);
        assertThat(frozenAccounts.getFirst().getAccountId()).isEqualTo("acct-f1");
    }
}
