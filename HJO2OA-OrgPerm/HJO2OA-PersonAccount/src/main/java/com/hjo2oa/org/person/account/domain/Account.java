package com.hjo2oa.org.person.account.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Account(
        UUID id,
        UUID personId,
        String username,
        String credential,
        AccountType accountType,
        boolean primaryAccount,
        boolean locked,
        Instant lockedUntil,
        Instant lastLoginAt,
        String lastLoginIp,
        Instant passwordChangedAt,
        boolean mustChangePassword,
        AccountStatus status,
        UUID tenantId,
        Instant createdAt,
        Instant updatedAt
) {

    public Account {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        username = Person.requireText(username, "username");
        credential = Person.requireText(credential, "credential");
        Objects.requireNonNull(accountType, "accountType must not be null");
        lastLoginIp = Person.normalizeNullable(lastLoginIp);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Account create(
            UUID id,
            UUID personId,
            String username,
            String credential,
            AccountType accountType,
            boolean primaryAccount,
            boolean mustChangePassword,
            UUID tenantId,
            Instant now
    ) {
        return new Account(
                id,
                personId,
                username,
                credential,
                accountType,
                primaryAccount,
                false,
                null,
                null,
                null,
                accountType == AccountType.PASSWORD ? now : null,
                mustChangePassword,
                AccountStatus.ACTIVE,
                tenantId,
                now,
                now
        );
    }

    public Account updateCredential(String credential, boolean mustChangePassword, Instant now) {
        return new Account(
                id,
                personId,
                username,
                credential,
                accountType,
                primaryAccount,
                locked,
                lockedUntil,
                lastLoginAt,
                lastLoginIp,
                now,
                mustChangePassword,
                status,
                tenantId,
                createdAt,
                now
        );
    }

    public Account markPrimary(boolean primary, Instant now) {
        if (primaryAccount == primary) {
            return this;
        }
        return copy(primary, locked, lockedUntil, status, now);
    }

    public Account lock(Instant lockedUntil, Instant now) {
        return copy(primaryAccount, true, lockedUntil, status, now);
    }

    public Account unlock(Instant now) {
        return copy(primaryAccount, false, null, status, now);
    }

    public Account disable(Instant now) {
        return copy(primaryAccount, true, lockedUntil, AccountStatus.DISABLED, now);
    }

    public Account activate(Instant now) {
        return copy(primaryAccount, false, null, AccountStatus.ACTIVE, now);
    }

    public Account recordLogin(String loginIp, Instant now) {
        return new Account(
                id,
                personId,
                username,
                credential,
                accountType,
                primaryAccount,
                locked,
                lockedUntil,
                now,
                loginIp,
                passwordChangedAt,
                mustChangePassword,
                status,
                tenantId,
                createdAt,
                now
        );
    }

    public AccountView toView() {
        return new AccountView(
                id,
                personId,
                username,
                accountType,
                primaryAccount,
                locked,
                lockedUntil,
                lastLoginAt,
                lastLoginIp,
                passwordChangedAt,
                mustChangePassword,
                status,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private Account copy(
            boolean primary,
            boolean lockedValue,
            Instant lockedUntilValue,
            AccountStatus targetStatus,
            Instant now
    ) {
        return new Account(
                id,
                personId,
                username,
                credential,
                accountType,
                primary,
                lockedValue,
                lockedUntilValue,
                lastLoginAt,
                lastLoginIp,
                passwordChangedAt,
                mustChangePassword,
                targetStatus,
                tenantId,
                createdAt,
                now
        );
    }
}
