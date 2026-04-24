package com.hjo2oa.portal.portal.model.interfaces;

import com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand;
import jakarta.validation.constraints.Min;

public record PublishPortalTemplateVersionRequest(
        @Min(1) int versionNo
) {

    public PublishPortalTemplateVersionCommand toCommand(String templateId) {
        return new PublishPortalTemplateVersionCommand(templateId, versionNo);
    }
}
