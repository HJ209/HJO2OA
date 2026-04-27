package com.hjo2oa.org.person.account.interfaces;

import com.hjo2oa.org.person.account.application.PersonAccountCommands;
import com.hjo2oa.org.person.account.domain.AccountStatus;
import com.hjo2oa.org.person.account.domain.AccountType;
import com.hjo2oa.org.person.account.domain.PersonStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PersonAccountDtos {

    private PersonAccountDtos() {
    }

    public record CreatePersonRequest(
            @NotBlank @Size(max = 64) String employeeNo,
            @NotBlank @Size(max = 64) String name,
            @Size(max = 128) String pinyin,
            @Size(max = 16) String gender,
            @Size(max = 32) String mobile,
            @Size(max = 128) String email,
            @NotNull UUID organizationId,
            UUID departmentId,
            UUID tenantId
    ) {

        public PersonAccountCommands.CreatePersonCommand toCommand() {
            return new PersonAccountCommands.CreatePersonCommand(
                    employeeNo,
                    name,
                    pinyin,
                    gender,
                    mobile,
                    email,
                    organizationId,
                    departmentId,
                    tenantId
            );
        }
    }

    public record UpdatePersonRequest(
            @NotBlank @Size(max = 64) String name,
            @Size(max = 128) String pinyin,
            @Size(max = 16) String gender,
            @Size(max = 32) String mobile,
            @Size(max = 128) String email,
            @NotNull UUID organizationId,
            UUID departmentId
    ) {

        public PersonAccountCommands.UpdatePersonCommand toCommand(UUID personId) {
            return new PersonAccountCommands.UpdatePersonCommand(
                    personId,
                    name,
                    pinyin,
                    gender,
                    mobile,
                    email,
                    organizationId,
                    departmentId
            );
        }
    }

    public record CreateAccountRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 256) String credential,
            @NotNull AccountType accountType,
            boolean primaryAccount,
            boolean mustChangePassword
    ) {

        public PersonAccountCommands.CreateAccountCommand toCommand(UUID personId) {
            return new PersonAccountCommands.CreateAccountCommand(
                    personId,
                    username,
                    credential,
                    accountType,
                    primaryAccount,
                    mustChangePassword
            );
        }
    }

    public record UpdateAccountCredentialRequest(
            @NotBlank @Size(max = 256) String credential,
            boolean mustChangePassword
    ) {

        public PersonAccountCommands.UpdateAccountCredentialCommand toCommand(UUID accountId) {
            return new PersonAccountCommands.UpdateAccountCredentialCommand(
                    accountId,
                    credential,
                    mustChangePassword
            );
        }
    }

    public record LockAccountRequest(
            Instant lockedUntil
    ) {

        public PersonAccountCommands.LockAccountCommand toCommand(UUID accountId) {
            return new PersonAccountCommands.LockAccountCommand(accountId, lockedUntil);
        }
    }

    public record LoginRecordRequest(
            @Size(max = 64) String loginIp
    ) {

        public PersonAccountCommands.RecordLoginCommand toCommand(UUID accountId) {
            return new PersonAccountCommands.RecordLoginCommand(accountId, loginIp);
        }
    }

    public record PersonResponse(
            UUID id,
            String employeeNo,
            String name,
            String pinyin,
            String gender,
            String mobile,
            String email,
            UUID organizationId,
            UUID departmentId,
            PersonStatus status,
            UUID tenantId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record AccountResponse(
            UUID id,
            UUID personId,
            String username,
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
    }

    public record PersonAccountResponse(
            PersonResponse person,
            List<AccountResponse> accounts
    ) {
    }
}
