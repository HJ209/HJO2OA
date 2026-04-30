package com.hjo2oa.org.org.structure.application;

import java.util.UUID;

public interface OrgStructureReferenceValidator {

    OrgStructureReferenceValidator NOOP = new OrgStructureReferenceValidator() {
    };

    default void ensureOrganizationCanBeDeleted(UUID tenantId, UUID organizationId) {
    }

    default void ensureDepartmentCanBeDeleted(UUID tenantId, UUID departmentId) {
    }
}
