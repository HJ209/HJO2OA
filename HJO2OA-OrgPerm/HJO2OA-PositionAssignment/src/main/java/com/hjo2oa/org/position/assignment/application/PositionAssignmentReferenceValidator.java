package com.hjo2oa.org.position.assignment.application;

import java.util.UUID;

public interface PositionAssignmentReferenceValidator {

    PositionAssignmentReferenceValidator NOOP = new PositionAssignmentReferenceValidator() {
    };

    default void ensurePositionScopeActive(UUID tenantId, UUID organizationId, UUID departmentId) {
    }

    default void ensurePersonAssignable(UUID tenantId, UUID personId) {
    }
}
