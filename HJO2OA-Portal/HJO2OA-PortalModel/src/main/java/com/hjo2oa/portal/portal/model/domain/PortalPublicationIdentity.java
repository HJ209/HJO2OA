package com.hjo2oa.portal.portal.model.domain;

import java.util.ArrayList;
import java.util.List;

public record PortalPublicationIdentity(
        String assignmentId,
        String positionId,
        String personId
) {

    public PortalPublicationIdentity {
        assignmentId = normalize(assignmentId, "assignmentId");
        positionId = normalize(positionId, "positionId");
        personId = normalize(personId, "personId");
    }

    public static PortalPublicationIdentity tenantDefault() {
        return new PortalPublicationIdentity(null, null, null);
    }

    public List<PortalPublicationAudience> candidateAudiences() {
        List<PortalPublicationAudience> audiences = new ArrayList<>(4);
        if (assignmentId != null) {
            audiences.add(PortalPublicationAudience.ofAssignment(assignmentId));
        }
        if (personId != null) {
            audiences.add(PortalPublicationAudience.ofPerson(personId));
        }
        if (positionId != null) {
            audiences.add(PortalPublicationAudience.ofPosition(positionId));
        }
        audiences.add(PortalPublicationAudience.tenantDefault());
        return List.copyOf(audiences);
    }

    private static String normalize(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
