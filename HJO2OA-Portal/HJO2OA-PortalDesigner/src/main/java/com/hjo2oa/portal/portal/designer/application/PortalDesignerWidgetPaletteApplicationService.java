package com.hjo2oa.portal.portal.designer.application;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerWidgetPaletteEntryState;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerWidgetPaletteEntryView;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerWidgetPaletteProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerWidgetPaletteProjectionRepository;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerWidgetPaletteView;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PortalDesignerWidgetPaletteApplicationService {

    private final PortalDesignerWidgetPaletteProjectionRepository projectionRepository;

    public PortalDesignerWidgetPaletteApplicationService(
            PortalDesignerWidgetPaletteProjectionRepository projectionRepository
    ) {
        this.projectionRepository = Objects.requireNonNull(
                projectionRepository,
                "projectionRepository must not be null"
        );
    }

    public PortalDesignerWidgetPaletteView currentPalette() {
        List<PortalDesignerWidgetPaletteEntryView> entries = projectionRepository.findAll().stream()
                .map(PortalDesignerWidgetPaletteProjection::toView)
                .sorted(Comparator.comparing(PortalDesignerWidgetPaletteEntryView::widgetCode)
                        .thenComparing(PortalDesignerWidgetPaletteEntryView::widgetId))
                .toList();
        List<PortalDesignerWidgetPaletteEntryView> activeWidgets = entries.stream()
                .filter(entry -> entry.state() == PortalDesignerWidgetPaletteEntryState.ACTIVE)
                .toList();
        List<PortalDesignerWidgetPaletteEntryView> disabledWidgets = entries.stream()
                .filter(entry -> entry.state() == PortalDesignerWidgetPaletteEntryState.DISABLED)
                .toList();
        return new PortalDesignerWidgetPaletteView(
                activeWidgets,
                disabledWidgets,
                entries.size()
        );
    }

    public PortalDesignerWidgetPaletteEntryView markUpdated(PortalWidgetUpdatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return projectionRepository.save(PortalDesignerWidgetPaletteProjection.active(event)).toView();
    }

    public PortalDesignerWidgetPaletteEntryView markDisabled(PortalWidgetDisabledEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return projectionRepository.save(PortalDesignerWidgetPaletteProjection.disabled(event)).toView();
    }
}
