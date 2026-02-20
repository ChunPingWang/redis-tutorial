package com.tutorial.redis.module13.domain.port.inbound;

import com.tutorial.redis.module13.domain.model.AclUser;
import com.tutorial.redis.module13.domain.model.EvictionPolicy;

import java.util.List;

/**
 * Inbound port for security information use cases.
 *
 * <p>Exposes operations for querying Redis ACL configuration and
 * available eviction policies.</p>
 */
public interface SecurityInfoUseCase {

    /**
     * Lists all configured ACL users.
     *
     * @return list of ACL users with their permissions
     */
    List<AclUser> listAclUsers();

    /**
     * Lists all available Redis eviction policies.
     *
     * @return all values of the {@link EvictionPolicy} enum
     */
    List<EvictionPolicy> listEvictionPolicies();
}
