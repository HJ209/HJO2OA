package com.hjo2oa.portal.portal.model.interfaces;

import com.hjo2oa.portal.portal.model.application.PortalWidgetReferenceStatusApplicationService;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PortalWidgetReferenceEventListener {

    private final PortalWidgetReferenceStatusApplicationService applicationService;

    public PortalWidgetReferenceEventListener(
            PortalWidgetReferenceStatusApplicationService applicationService
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
