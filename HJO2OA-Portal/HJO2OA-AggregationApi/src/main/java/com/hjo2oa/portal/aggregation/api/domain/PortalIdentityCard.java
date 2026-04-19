package com.hjo2oa.portal.aggregation.api.domain;

import java.time.Instant;
import java.util.Objects;

public record PortalIdentityCard(
        String tenantId,
        String personId,
        String accountId,
        String assignmentId,
        String positionId,
        String organizationId,
        String departmentId,
        String positionName,
        String organizationName,
        String departmentName,
        String assignmentType,
        Instant effectiveAt
) {

    public PortalIdentityCard {
        tenantId = requireText(tenantId, "tenantId");
        personId = requireText(personId, "personId");
        accountId = requireText(accountId, "accountId");
        assignmentId = requireText(assignmentId, "assignmentId");
        positionId = requireText(positionId, "positionId");
        organizationId = requireText(organizationId, "organizationId");
        departmentId = requireText(departmentId, "departmentId");
        positionName = requireText(positionName, "positionName");
        organizationName = requireText(organizationName, "organizationName");
        departmentName = requireText(departmentName, "departmentName");
        assignmentType = requireText(assignmentType, "assignmentType");
        Objects.requireNonNull(effectiveAt, "effectiveAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
