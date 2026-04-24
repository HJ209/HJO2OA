package com.hjo2oa.data.common.security;

import com.hjo2oa.data.common.support.Require;
import java.util.Set;

public record DataPrincipal(
        String tenantId,
        String accountId,
        Set<String> roles
) {

    public DataPrincipal {
        tenantId = Require.text(tenantId, "tenantId");
        accountId = Require.text(accountId, "accountId");
        roles = Set.copyOf(Require.nonNull(roles, "roles"));
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
