package com.tutorial.redis.module13.application.usecase;

import com.tutorial.redis.module13.domain.model.AclUser;
import com.tutorial.redis.module13.domain.model.EvictionPolicy;
import com.tutorial.redis.module13.domain.port.inbound.SecurityInfoUseCase;
import com.tutorial.redis.module13.domain.port.outbound.SecurityInfoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Application service implementing security information use cases.
 *
 * <p>Delegates ACL queries to the {@link SecurityInfoPort} adapter and
 * provides a static listing of available eviction policies from the
 * {@link EvictionPolicy} enum.</p>
 */
@Service
public class SecurityInfoService implements SecurityInfoUseCase {

    private static final Logger log = LoggerFactory.getLogger(SecurityInfoService.class);

    private final SecurityInfoPort securityInfoPort;

    public SecurityInfoService(SecurityInfoPort securityInfoPort) {
        this.securityInfoPort = securityInfoPort;
    }

    @Override
    public List<AclUser> listAclUsers() {
        log.info("Listing ACL users");
        return securityInfoPort.listAclUsers();
    }

    @Override
    public List<EvictionPolicy> listEvictionPolicies() {
        log.info("Listing eviction policies");
        return Arrays.asList(EvictionPolicy.values());
    }
}
