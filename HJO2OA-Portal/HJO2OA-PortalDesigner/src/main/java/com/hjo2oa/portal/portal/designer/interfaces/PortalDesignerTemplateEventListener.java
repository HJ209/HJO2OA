package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateDeprecatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedEvent;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PortalDesignerTemplateEventListener {

    private final PortalDesignerTemplateProjectionRepository projectionRepository;

    public PortalDesignerTemplateEventListener(
            PortalDesignerTemplateProjectionRepository projectionRepository
    ) {
        this.projectionRepository = Objects.requireNonNull(
                projectionRepository,
                "projectionRepository must not be null"
        );
    }

    @EventListener
    public void onTemplateCreated(PortalTemplateCreatedEvent event) {
        projectionRepository.save(
                projectionRepository.findByTemplateId(event.templateId())
                        .orElseGet(() -> PortalDesignerTemplateProjection.initialize(event))
        );
    }

    @EventListener
    public void onTemplatePublished(PortalTemplatePublishedEvent event) {
        projectionRepository.findByTemplateId(event.templateId())
                .ifPresent(projection -> projectionRepository.save(projection.applyTemplatePublished(event)));
    }

    @EventListener
    public void onTemplateDeprecated(PortalTemplateDeprecatedEvent event) {
        projectionRepository.findByTemplateId(event.templateId())
                .ifPresent(projection -> projectionRepository.save(projection.applyTemplateDeprecated(event)));
    }

    @EventListener
    public void onPublicationActivated(PortalPublicationActivatedEvent event) {
        projectionRepository.findByTemplateId(event.templateId())
                .ifPresent(projection -> projectionRepository.save(projection.activatePublication(event)));
    }

    @EventListener
    public void onPublicationOfflined(PortalPublicationOfflinedEvent event) {
        projectionRepository.findByTemplateId(event.templateId())
                .ifPresent(projection -> projectionRepository.save(projection.offlinePublication(event)));
    }
}
