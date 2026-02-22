package com.tutorial.redis.module13.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module13.domain.model.AclUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試 RedisSecurityInfoAdapter 的整合行為（Adapter 層）。
 * 驗證透過實際 Redis 連線查詢 ACL 用戶清單及 INFO 各區段資訊，
 * 確保安全相關資料能正確從 Redis 取得並轉換為領域模型。
 * 屬於六角形架構的 Outbound Adapter 層。
 */
@DisplayName("RedisSecurityInfoAdapter 整合測試")
class RedisSecurityInfoAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisSecurityInfoAdapter adapter;

    // 驗證查詢 ACL LIST 時，至少能取得 Redis 預設的 default 用戶
    @Test
    @DisplayName("listAclUsers_ReturnsAtLeastDefaultUser — 列出 ACL 用戶至少應包含 default 用戶")
    void listAclUsers_ReturnsAtLeastDefaultUser() {
        // Act
        List<AclUser> result = adapter.listAclUsers();

        // Assert — every Redis instance has at least a "default" ACL user
        assertThat(result).isNotEmpty();
        assertThat(result).anyMatch(u -> u.getUsername().equals("default"));
    }

    // 驗證查詢 INFO server 區段時，回傳的 Map 非空且包含 redis_version 欄位
    @Test
    @DisplayName("getServerInfo_ReturnsInfoSection — 查詢 server 區段，應回傳非空 Map")
    void getServerInfo_ReturnsInfoSection() {
        // Act
        Map<String, String> result = adapter.getServerInfo("server");

        // Assert — the "server" section should contain redis_version
        assertThat(result).isNotEmpty().containsKey("redis_version");
    }
}
