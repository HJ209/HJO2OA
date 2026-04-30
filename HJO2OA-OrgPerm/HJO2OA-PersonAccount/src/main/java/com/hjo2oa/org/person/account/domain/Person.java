package com.hjo2oa.org.person.account.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Person(
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

    public Person {
        Objects.requireNonNull(id, "id must not be null");
        employeeNo = requireText(employeeNo, "employeeNo");
        name = requireText(name, "name");
        pinyin = normalizeNullable(pinyin);
        gender = normalizeNullable(gender);
        mobile = normalizeNullable(mobile);
        email = normalizeNullable(email);
        Objects.requireNonNull(organizationId, "organizationId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Person create(
            UUID id,
            String employeeNo,
            String name,
            String pinyin,
            String gender,
            String mobile,
            String email,
            UUID organizationId,
            UUID departmentId,
            UUID tenantId,
            Instant now
    ) {
        return new Person(
                id,
                employeeNo,
                name,
                pinyin,
                gender,
                mobile,
                email,
                organizationId,
                departmentId,
                PersonStatus.ACTIVE,
                tenantId,
                now,
                now
        );
    }

    public Person updateProfile(
            String name,
            String pinyin,
            String gender,
            String mobile,
            String email,
            UUID organizationId,
            UUID departmentId,
            Instant now
    ) {
        return new Person(
                id,
                employeeNo,
                name,
                pinyin,
                gender,
                mobile,
                email,
                organizationId,
                departmentId,
                status,
                tenantId,
                createdAt,
                now
        );
    }

    public Person disable(Instant now) {
        return changeStatus(PersonStatus.DISABLED, now);
    }

    public Person activate(Instant now) {
        return changeStatus(PersonStatus.ACTIVE, now);
    }

    public Person resign(Instant now) {
        return changeStatus(PersonStatus.RESIGNED, now);
    }

    public PersonView toView() {
        return new PersonView(
                id,
                employeeNo,
                name,
                pinyin,
                gender,
                mobile,
                email,
                organizationId,
                departmentId,
                status,
                tenantId,
                createdAt,
                updatedAt
        );
    }

    private Person changeStatus(PersonStatus targetStatus, Instant now) {
        if (status == targetStatus) {
            return this;
        }
        return new Person(
                id,
                employeeNo,
                name,
                pinyin,
                gender,
                mobile,
                email,
                organizationId,
                departmentId,
                targetStatus,
                tenantId,
                createdAt,
                now
        );
    }

    static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
