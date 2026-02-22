package com.tutorial.redis.module13.application.usecase;

import com.tutorial.redis.module13.domain.model.AclUser;
import com.tutorial.redis.module13.domain.model.EvictionPolicy;
import com.tutorial.redis.module13.domain.port.outbound.SecurityInfoPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 測試 SecurityInfoService 的應用層邏輯（Application 層）。
 * 驗證安全資訊服務能正確列出 Redis 淘汰策略（Eviction Policy）
 * 及透過 SecurityInfoPort 委派查詢 ACL 用戶清單的功能。
 * 屬於六角形架構的 Application（Use Case）層。
 */
@DisplayName("SecurityInfoService 單元測試")
@ExtendWith(MockitoExtension.class)
class SecurityInfoServiceTest {

    @Mock
    private SecurityInfoPort port;

    @InjectMocks
    private SecurityInfoService service;

    // 驗證列出所有 Redis 淘汰策略時，應回傳完整的 8 種策略
    @Test
    @DisplayName("listEvictionPolicies_ReturnsEightPolicies — 列出淘汰策略應回傳 8 種")
    void listEvictionPolicies_ReturnsEightPolicies() {
        // Act
        List<EvictionPolicy> policies = service.listEvictionPolicies();

        // Assert — Redis has exactly 8 eviction policies
        assertThat(policies).hasSize(8);
    }

    // 驗證 listAclUsers() 正確委派給 SecurityInfoPort，並回傳包含 default 用戶的清單
    @Test
    @DisplayName("listAclUsers_DelegatesToPort — 列出 ACL 用戶應委派給 SecurityInfoPort")
    void listAclUsers_DelegatesToPort() {
        // Arrange — stub the port to return a list with the default user
        AclUser defaultUser = new AclUser("default", true,
                List.of("+@all"), List.of("~*"), List.of("&*"));
        when(port.listAclUsers()).thenReturn(List.of(defaultUser));

        // Act
        List<AclUser> result = service.listAclUsers();

        // Assert — verify delegation and correct result
        verify(port).listAclUsers();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("default");
    }
}
