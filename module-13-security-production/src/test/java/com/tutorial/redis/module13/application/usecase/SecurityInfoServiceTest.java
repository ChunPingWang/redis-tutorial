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

@DisplayName("SecurityInfoService 單元測試")
@ExtendWith(MockitoExtension.class)
class SecurityInfoServiceTest {

    @Mock
    private SecurityInfoPort port;

    @InjectMocks
    private SecurityInfoService service;

    @Test
    @DisplayName("listEvictionPolicies_ReturnsEightPolicies — 列出淘汰策略應回傳 8 種")
    void listEvictionPolicies_ReturnsEightPolicies() {
        // Act
        List<EvictionPolicy> policies = service.listEvictionPolicies();

        // Assert — Redis has exactly 8 eviction policies
        assertThat(policies).hasSize(8);
    }

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
