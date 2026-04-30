package com.hjo2oa.org.data.permission.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record DataPermissionIdentityContext(
        UUID tenantId,
        UUID personId,
        UUID organizationId,
        UUID departmentId,
        UUID positionId,
        List<UUID> roleIds
) {

    public DataPermissionIdentityContext {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(positionId, "positionId must not be null");
        roleIds = List.copyOf(new LinkedHashSet<>(Objects.requireNonNullElse(roleIds, List.of())));
    }

    public List<SubjectReference> toSubjects() {
        List<SubjectReference> subjects = new ArrayList<>();
        if (organizationId != null) {
            subjects.add(new SubjectReference(PermissionSubjectType.ORGANIZATION, organizationId));
        }
        if (departmentId != null) {
            subjects.add(new SubjectReference(PermissionSubjectType.DEPARTMENT, departmentId));
        }
        subjects.add(new SubjectReference(PermissionSubjectType.POSITION, positionId));
        subjects.add(new SubjectReference(PermissionSubjectType.PERSON, personId));
        for (UUID roleId : roleIds) {
            subjects.add(new SubjectReference(PermissionSubjectType.ROLE, roleId));
        }
        return subjects;
    }
}
