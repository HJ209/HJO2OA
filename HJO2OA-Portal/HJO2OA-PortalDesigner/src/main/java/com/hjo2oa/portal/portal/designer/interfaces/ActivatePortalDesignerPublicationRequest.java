package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import jakarta.validation.constraints.NotNull;

public record ActivatePortalDesignerPublicationRequest(
        @NotNull PortalPublicationClientType clientType
) {
}
