package com.hjo2oa.portal.portal.model.application;

import com.hjo2oa.portal.portal.model.domain.PortalPage;
import java.util.List;
import java.util.Objects;

public record SavePortalTemplateCanvasCommand(
        String templateId,
        List<PortalPage> pages
) {

    public SavePortalTemplateCanvasCommand {
        Objects.requireNonNull(templateId, "templateId must not be null");
        pages = List.copyOf(Objects.requireNonNull(pages, "pages must not be null"));
    }
}
