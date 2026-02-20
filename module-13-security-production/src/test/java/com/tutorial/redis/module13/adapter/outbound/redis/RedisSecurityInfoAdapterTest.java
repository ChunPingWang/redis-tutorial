package com.tutorial.redis.module13.adapter.outbound.redis;

import com.tutorial.redis.common.test.AbstractRedisIntegrationTest;
import com.tutorial.redis.module13.domain.model.AclUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisSecurityInfoAdapter 整合測試")
class RedisSecurityInfoAdapterTest extends AbstractRedisIntegrationTest {

    @Autowired
    private RedisSecurityInfoAdapter adapter;

    @Test
    @DisplayName("listAclUsers_ReturnsAtLeastDefaultUser — 列出 ACL 用戶至少應包含 default 用戶")
    void listAclUsers_ReturnsAtLeastDefaultUser() {
        // Act
        List<AclUser> result = adapter.listAclUsers();

        // Assert — every Redis instance has at least a "default" ACL user
        assertThat(result).isNotEmpty();
        assertThat(result).anyMatch(u -> u.getUsername().equals("default"));
    }

    @Test
    @DisplayName("getServerInfo_ReturnsInfoSection — 查詢 server 區段，應回傳非空 Map")
    void getServerInfo_ReturnsInfoSection() {
        // Act
        Map<String, String> result = adapter.getServerInfo("server");

        // Assert — the "server" section should contain redis_version
        assertThat(result).isNotEmpty().containsKey("redis_version");
    }
}
