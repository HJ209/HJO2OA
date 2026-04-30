package com.hjo2oa.org.person.account.application;

import java.util.UUID;

public interface PersonAccountReferenceValidator {

    PersonAccountReferenceValidator NOOP = new PersonAccountReferenceValidator() {
    };

    default void ensureOrgScopeActive(UUID tenantId, UUID organizationId, UUID departmentId) {
    }

    default void ensurePersonCanBeDeleted(UUID tenantId, UUID personId) {
    }
}
