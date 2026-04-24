package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;

public record PortalDesignerPreviewIdentityView(
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
        String assignmentType
) {

    public static PortalDesignerPreviewIdentityView from(PortalIdentityCard identityCard) {
        return new PortalDesignerPreviewIdentityView(
                identityCard.tenantId(),
                identityCard.personId(),
                identityCard.accountId(),
                identityCard.assignmentId(),
                identityCard.positionId(),
                identityCard.organizationId(),
                identityCard.departmentId(),
                identityCard.positionName(),
                identityCard.organizationName(),
                identityCard.departmentName(),
                identityCard.assignmentType()
        );
    }
}
