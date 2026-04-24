package com.hjo2oa.portal.portal.model.interfaces;

import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActivatePortalPublicationRequest(
        @NotBlank @Size(max = 128) String templateId,
        @NotNull PortalPublicationSceneType sceneType,
        @NotNull PortalPublicationClientType clientType,
        @Size(max = 128) String assignmentId,
        @Size(max = 128) String positionId,
        @Size(max = 128) String personId
) {

    public ActivatePortalPublicationCommand toCommand(String publicationId) {
        return new ActivatePortalPublicationCommand(
                publicationId,
                templateId,
                sceneType,
                clientType,
                PortalPublicationAudience.from(assignmentId, positionId, personId)
        );
    }
}
