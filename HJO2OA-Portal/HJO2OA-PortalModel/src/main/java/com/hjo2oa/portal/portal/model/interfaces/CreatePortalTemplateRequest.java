package com.hjo2oa.portal.portal.model.interfaces;

import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePortalTemplateRequest(
        @NotBlank @Size(max = 128) String templateId,
        @NotBlank @Size(max = 128) String templateCode,
        @NotBlank @Size(max = 256) String displayName,
        @NotNull PortalPublicationSceneType sceneType
) {

    public CreatePortalTemplateCommand toCommand() {
        return new CreatePortalTemplateCommand(templateId, templateCode, displayName, sceneType);
    }
}
