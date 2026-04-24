package com.hjo2oa.portal.portal.model.domain;

import java.util.Objects;

public record PortalPublicationAudience(
        PortalPublicationAudienceType type,
        String subjectId
) {

    public PortalPublicationAudience {
        Objects.requireNonNull(type, "type must not be null");
        subjectId = normalize(subjectId);
        if (type == PortalPublicationAudienceType.TENANT_DEFAULT) {
            if (subjectId != null) {
                throw new IllegalArgumentException("tenant-default audience must not specify subjectId");
            }
        } else if (subjectId == null) {
            throw new IllegalArgumentException("subjectId must not be null for scoped audience");
        }
    }

    public static PortalPublicationAudience from(
            String assignmentId,
            String positionId,
            String personId
    ) {
        PortalPublicationAudience audience = null;
        audience = addAudience(audience, assignmentId == null ? null : ofAssignment(assignmentId));
        audience = addAudience(audience, personId == null ? null : ofPerson(personId));
        audience = addAudience(audience, positionId == null ? null : ofPosition(positionId));
        return audience == null ? tenantDefault() : audience;
    }

    public static PortalPublicationAudience tenantDefault() {
        return new PortalPublicationAudience(PortalPublicationAudienceType.TENANT_DEFAULT, null);
    }

    public static PortalPublicationAudience ofAssignment(String assignmentId) {
        return new PortalPublicationAudience(PortalPublicationAudienceType.ASSIGNMENT, assignmentId);
    }

    public static PortalPublicationAudience ofPerson(String personId) {
        return new PortalPublicationAudience(PortalPublicationAudienceType.PERSON, personId);
    }

    public static PortalPublicationAudience ofPosition(String positionId) {
        return new PortalPublicationAudience(PortalPublicationAudienceType.POSITION, positionId);
    }

    private static PortalPublicationAudience addAudience(
            PortalPublicationAudience current,
            PortalPublicationAudience next
    ) {
        if (next == null) {
            return current;
        }
        if (current != null) {
            throw new IllegalArgumentException("publication audience must target exactly one scope");
        }
        return next;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("audience subjectId must not be blank");
        }
        return normalized;
    }
}
