package com.hjo2oa.portal.portal.designer.application;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateWidgetPaletteItemView;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateWidgetPaletteView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.widget.config.application.WidgetDefinitionApplicationService;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionView;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PortalDesignerTemplateWidgetPaletteApplicationService {

    private final PortalDesignerTemplateProjectionRepository projectionRepository;
    private final WidgetDefinitionApplicationService widgetDefinitionApplicationService;

    public PortalDesignerTemplateWidgetPaletteApplicationService(
            PortalDesignerTemplateProjectionRepository projectionRepository,
            WidgetDefinitionApplicationService widgetDefinitionApplicationService
    ) {
        this.projectionRepository = Objects.requireNonNull(
                projectionRepository,
                "projectionRepository must not be null"
        );
        this.widgetDefinitionApplicationService = Objects.requireNonNull(
                widgetDefinitionApplicationService,
                "widgetDefinitionApplicationService must not be null"
        );
    }

    public Optional<PortalDesignerTemplateWidgetPaletteView> current(String templateId) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        return projectionRepository.findByTemplateId(templateId)
                .map(this::toPaletteView);
    }

    private PortalDesignerTemplateWidgetPaletteView toPaletteView(PortalDesignerTemplateProjection projection) {
        List<PortalDesignerTemplateWidgetPaletteItemView> widgets = widgetDefinitionApplicationService
                .list(mapSceneType(projection.sceneType()), null)
                .stream()
                .map(this::toItemView)
                .toList();
        return new PortalDesignerTemplateWidgetPaletteView(
                projection.templateId(),
                projection.templateCode(),
                projection.sceneType(),
                widgets
        );
    }

    private PortalDesignerTemplateWidgetPaletteItemView toItemView(WidgetDefinitionView widget) {
        return new PortalDesignerTemplateWidgetPaletteItemView(
                widget.widgetId(),
                widget.widgetCode(),
                widget.displayName(),
                widget.cardType(),
                widget.sceneType(),
                widget.sourceModule(),
                widget.dataSourceType(),
                widget.allowHide(),
                widget.allowCollapse(),
                widget.maxItems(),
                widget.status()
        );
    }

    private WidgetSceneType mapSceneType(PortalPublicationSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> WidgetSceneType.HOME;
            case OFFICE_CENTER -> WidgetSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> WidgetSceneType.MOBILE_WORKBENCH;
        };
    }
}
