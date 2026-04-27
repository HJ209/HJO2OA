package com.hjo2oa.org.data.permission.domain;

import java.util.Objects;
import java.util.UUID;

public record SubjectReference(PermissionSubjectType subjectType, UUID subjectId) {

    public SubjectReference {
        Objects.requireNonNull(subjectType, "subjectType must not be null");
        Objects.requireNonNull(subjectId, "subjectId must not be null");
    }
}
