package com.hjo2oa.data.common.security;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextDataPrincipalProvider implements DataPrincipalProvider {

    private static final DataPrincipal FALLBACK_PRINCIPAL =
            new DataPrincipal("tenant-local", "system", Set.of("SYSTEM"));

    @Override
    public DataPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return FALLBACK_PRINCIPAL;
        }
        String tenantId = authentication.getDetails() instanceof String detail && !detail.isBlank()
                ? detail
                : "tenant-local";
        return new DataPrincipal(
                tenantId,
                authentication.getName(),
                authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .collect(Collectors.toSet())
        );
    }
}
