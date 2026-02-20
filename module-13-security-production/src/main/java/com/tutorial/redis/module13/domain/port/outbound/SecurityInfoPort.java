package com.tutorial.redis.module13.domain.port.outbound;

import com.tutorial.redis.module13.domain.model.AclUser;

import java.util.List;
import java.util.Map;

/**
 * Outbound port for Redis security information queries.
 *
 * <p>Implemented by a Redis adapter that invokes ACL commands and
 * INFO sections to retrieve security-related server configuration.</p>
 */
public interface SecurityInfoPort {

    /**
     * Lists all configured ACL users by executing {@code ACL LIST}.
     *
     * @return list of parsed ACL users; returns a single default user
     *         if the command is not supported
     */
    List<AclUser> listAclUsers();

    /**
     * Retrieves a specific INFO section from the Redis server.
     *
     * @param section the INFO section name (e.g. "server", "memory", "clients")
     * @return key-value pairs from the requested INFO section
     */
    Map<String, String> getServerInfo(String section);
}
