package com.hjo2oa.infra.security.infrastructure.jwt;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtAuthenticationToken implements Authentication {

    private final JwtClaims claims;
    private final String token;
    private final List<GrantedAuthority> authorities;
    private boolean authenticated;

    public JwtAuthenticationToken(JwtClaims claims, String token) {
        this.claims = Objects.requireNonNull(claims, "claims must not be null");
        this.token = Objects.requireNonNull(token, "token must not be null");
        this.authorities = claims.roles().stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        this.authenticated = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getDetails() {
        return claims;
    }

    @Override
    public Object getPrincipal() {
        return claims.personId();
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return claims.username();
    }

    public JwtClaims claims() {
        return claims;
    }
}
