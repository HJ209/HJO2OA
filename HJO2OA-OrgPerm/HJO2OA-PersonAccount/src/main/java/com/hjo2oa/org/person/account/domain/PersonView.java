package com.hjo2oa.org.person.account.domain;

import java.time.Instant;
import java.util.UUID;

public record PersonView(
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
