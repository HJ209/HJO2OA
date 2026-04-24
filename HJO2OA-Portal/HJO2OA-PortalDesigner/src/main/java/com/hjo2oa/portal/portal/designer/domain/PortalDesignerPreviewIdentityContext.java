package com.hjo2oa.portal.portal.designer.domain;

public record PortalDesignerPreviewIdentityContext(
        String tenantId,
        String personId,
        String accountId,
        String assignmentId,
        String positionId
) {

    public static PortalDesignerPreviewIdentityContext of(
            String tenantId,
            String personId,
            String accountId,
            String assignmentId,
            String positionId
    ) {
        PortalDesignerPreviewIdentityContext context = new PortalDesignerPreviewIdentityContext(
                normalize(tenantId),
                normalize(personId),
                normalize(accountId),
                normalize(assignmentId),
                normalize(positionId)
        );
        return context.isEmpty() ? null : context;
    }

    public boolean isEmpty() {
        return tenantId == null
                && personId == null
                && accountId == null
                && assignmentId == null
                && positionId == null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
