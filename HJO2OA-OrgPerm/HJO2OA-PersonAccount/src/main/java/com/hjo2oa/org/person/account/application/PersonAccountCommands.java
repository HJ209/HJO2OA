package com.hjo2oa.org.person.account.application;

import com.hjo2oa.org.person.account.domain.AccountType;
import java.time.Instant;
import java.util.UUID;

public final class PersonAccountCommands {

    private PersonAccountCommands() {
    }

    public record CreatePersonCommand(
            String employeeNo,
            String name,
            String pinyin,
            String gender,
            String mobile,
            String email,
            UUID organizationId,
            UUID departmentId,
            UUID tenantId
    ) {
    }

    public record UpdatePersonCommand(
            UUID personId,
            String name,
            String pinyin,
            String gender,
            String mobile,
            String email,
            UUID organizationId,
            UUID departmentId,
            UUID tenantId
    ) {
    }

    public record CreateAccountCommand(
            UUID personId,
            String username,
            String credential,
            AccountType accountType,
            boolean primaryAccount,
            boolean mustChangePassword,
            UUID tenantId
    ) {
    }

    public record UpdateAccountCredentialCommand(
            UUID accountId,
            String credential,
            boolean mustChangePassword,
            UUID tenantId
    ) {
    }

    public record LockAccountCommand(
            UUID accountId,
            Instant lockedUntil,
            UUID tenantId
    ) {
    }

    public record RecordLoginCommand(
            UUID accountId,
            String loginIp,
            UUID tenantId
    ) {
    }
}
