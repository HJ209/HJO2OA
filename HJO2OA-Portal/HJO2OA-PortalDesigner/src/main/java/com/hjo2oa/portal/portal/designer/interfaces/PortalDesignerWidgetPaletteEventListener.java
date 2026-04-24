package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.designer.application.PortalDesignerWidgetPaletteApplicationService;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PortalDesignerWidgetPaletteEventListener {

    private final PortalDesignerWidgetPaletteApplicationService applicationService;

    public PortalDesignerWidgetPaletteEventListener(
            PortalDesignerWidgetPaletteApplicationService applicationService
    ) {
        this.applicationService = applicationService;
    }

    @EventListener
    public void onWidgetUpdated(PortalWidgetUpdatedEvent event) {
        applicationService.markUpdated(event);
    }

    @EventListener
    public void onWidgetDisabled(PortalWidgetDisabledEvent event) {
        applicationService.markDisabled(event);
    }
}
