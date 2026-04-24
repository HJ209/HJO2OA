package com.hjo2oa.portal.portal.designer.application;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateInitializationView;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateWidgetPaletteView;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCanvasView;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PortalDesignerTemplateInitializationApplicationService {

    private final PortalDesignerTemplateStatusApplicationService templateStatusApplicationService;
    private final PortalDesignerTemplateWidgetPaletteApplicationService templateWidgetPaletteApplicationService;
    private final PortalTemplateCanvasApplicationService templateCanvasApplicationService;

    public PortalDesignerTemplateInitializationApplicationService(
            PortalDesignerTemplateStatusApplicationService templateStatusApplicationService,
            PortalDesignerTemplateWidgetPaletteApplicationService templateWidgetPaletteApplicationService,
            PortalTemplateCanvasApplicationService templateCanvasApplicationService
    ) {
        this.templateStatusApplicationService = Objects.requireNonNull(
                templateStatusApplicationService,
                "templateStatusApplicationService must not be null"
        );
        this.templateWidgetPaletteApplicationService = Objects.requireNonNull(
                templateWidgetPaletteApplicationService,
                "templateWidgetPaletteApplicationService must not be null"
        );
        this.templateCanvasApplicationService = Objects.requireNonNull(
                templateCanvasApplicationService,
                "templateCanvasApplicationService must not be null"
        );
    }

    public Optional<PortalDesignerTemplateInitializationView> current(String templateId) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        return templateStatusApplicationService.current(templateId)
                .flatMap(status -> templateCanvasApplicationService.current(templateId)
                        .flatMap(canvas -> templateWidgetPaletteApplicationService.current(templateId)
                                .map(widgetPalette -> toView(status, canvas, widgetPalette))));
    }

    private PortalDesignerTemplateInitializationView toView(
            PortalDesignerTemplateStatusView status,
            PortalTemplateCanvasView canvas,
            PortalDesignerTemplateWidgetPaletteView widgetPalette
    ) {
        return new PortalDesignerTemplateInitializationView(
                status.templateId(),
                status.templateCode(),
                status.sceneType(),
                status,
                canvas,
                widgetPalette
        );
    }
}
