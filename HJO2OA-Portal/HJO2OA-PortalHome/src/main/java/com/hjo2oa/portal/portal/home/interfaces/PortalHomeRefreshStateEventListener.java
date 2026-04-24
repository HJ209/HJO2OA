package com.hjo2oa.portal.portal.home.interfaces;

import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotFailedEvent;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotRefreshedEvent;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidatedEvent;
import com.hjo2oa.org.identity.context.domain.IdentitySwitchedEvent;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationResetEvent;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationSavedEvent;
import com.hjo2oa.portal.portal.home.application.PortalHomeRefreshStateApplicationService;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import java.time.Instant;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PortalHomeRefreshStateEventListener {

    private final PortalHomeRefreshStateApplicationService refreshStateApplicationService;

    public PortalHomeRefreshStateEventListener(
            PortalHomeRefreshStateApplicationService refreshStateApplicationService
    ) {
        this.refreshStateApplicationService = refreshStateApplicationService;
    }

    @EventListener
    public void onSnapshotRefreshed(PortalSnapshotRefreshedEvent event) {
        refreshStateApplicationService.markCardRefreshed(
                event.tenantId(),
                event.personId(),
                event.assignmentId(),
                mapSceneType(event.sceneType()),
                event.cardType(),
                event.eventType(),
                event.refreshedAt()
        );
    }

    @EventListener
    public void onSnapshotFailed(PortalSnapshotFailedEvent event) {
        refreshStateApplicationService.markCardFailed(
                event.tenantId(),
                event.personId(),
                event.assignmentId(),
                mapSceneType(event.sceneType()),
                event.cardType(),
                event.eventType(),
                event.reason(),
                event.occurredAt()
        );
    }

    @EventListener
    public void onIdentitySwitched(IdentitySwitchedEvent event) {
        refreshStateApplicationService.markReloadRequiredAllScenesForPerson(
                event.tenantId(),
                event.personId(),
                event.eventType(),
                event.occurredAt()
        );
    }

    @EventListener
    public void onIdentityContextInvalidated(IdentityContextInvalidatedEvent event) {
        refreshStateApplicationService.markReloadRequiredAllScenesForPerson(
                event.tenantId(),
                event.personId(),
                event.eventType(),
                event.occurredAt()
        );
    }

    @EventListener
    public void onPublicationActivated(PortalPublicationActivatedEvent event) {
        markReloadRequiredForTenant(
                event.tenantId(),
                mapPublicationSceneType(event.sceneType()),
                event.eventType(),
                event.occurredAt()
        );
    }

    @EventListener
    public void onPublicationOfflined(PortalPublicationOfflinedEvent event) {
        markReloadRequiredForTenant(
                event.tenantId(),
                mapPublicationSceneType(event.sceneType()),
                event.eventType(),
                event.occurredAt()
        );
    }

    @EventListener
    public void onPersonalizationSaved(PortalPersonalizationSavedEvent event) {
        refreshStateApplicationService.markReloadRequiredForPerson(
                event.tenantId(),
                event.personId(),
                mapPersonalizationSceneType(event.sceneType()),
                event.eventType(),
                event.occurredAt()
        );
    }

    @EventListener
    public void onPersonalizationReset(PortalPersonalizationResetEvent event) {
        refreshStateApplicationService.markReloadRequiredForPerson(
                event.tenantId(),
                event.personId(),
                mapPersonalizationSceneType(event.sceneType()),
                event.eventType(),
                event.occurredAt()
        );
    }

    @EventListener
    public void onWidgetUpdated(PortalWidgetUpdatedEvent event) {
        if (event.sceneType() == null || transitionedToOrFromTenantWideWidget(event)) {
            refreshStateApplicationService.markReloadRequiredAllScenesForTenant(
                    event.tenantId(),
                    event.eventType(),
                    event.occurredAt()
            );
            return;
        }
        refreshStateApplicationService.markReloadRequiredForTenant(
                event.tenantId(),
                mapWidgetSceneType(event.sceneType()),
                event.eventType(),
                event.occurredAt()
        );
        if (sceneChangedBetweenSpecificScopes(event)) {
            refreshStateApplicationService.markReloadRequiredForTenant(
                    event.tenantId(),
                    mapWidgetSceneType(event.previousSceneType()),
                    event.eventType(),
                    event.occurredAt()
            );
        }
    }

    @EventListener
    public void onWidgetDisabled(PortalWidgetDisabledEvent event) {
        if (event.sceneType() == null) {
            refreshStateApplicationService.markReloadRequiredAllScenesForTenant(
                    event.tenantId(),
                    event.eventType(),
                    event.occurredAt()
            );
            return;
        }
        refreshStateApplicationService.markReloadRequiredForTenant(
                event.tenantId(),
                mapWidgetSceneType(event.sceneType()),
                event.eventType(),
                event.occurredAt()
        );
    }

    private void markReloadRequiredForTenant(
            String tenantId,
            PortalHomeSceneType sceneType,
            String triggerEvent,
            Instant occurredAt
    ) {
        if (sceneType == null) {
            refreshStateApplicationService.markReloadRequiredAllScenesForTenant(tenantId, triggerEvent, occurredAt);
            return;
        }
        refreshStateApplicationService.markReloadRequiredForTenant(tenantId, sceneType, triggerEvent, occurredAt);
    }

    private void markReloadRequiredForTenant(
            String tenantId,
            PortalSceneType sceneType,
            String triggerEvent,
            Instant occurredAt
    ) {
        markReloadRequiredForTenant(tenantId, mapSceneType(sceneType), triggerEvent, occurredAt);
    }

    private PortalHomeSceneType mapSceneType(PortalSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PortalHomeSceneType.HOME;
            case OFFICE_CENTER -> PortalHomeSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalHomeSceneType.MOBILE_WORKBENCH;
        };
    }

    private PortalHomeSceneType mapPersonalizationSceneType(PersonalizationSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PortalHomeSceneType.HOME;
            case OFFICE_CENTER -> PortalHomeSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalHomeSceneType.MOBILE_WORKBENCH;
        };
    }

    private PortalHomeSceneType mapWidgetSceneType(WidgetSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PortalHomeSceneType.HOME;
            case OFFICE_CENTER -> PortalHomeSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalHomeSceneType.MOBILE_WORKBENCH;
        };
    }

    private PortalHomeSceneType mapPublicationSceneType(PortalPublicationSceneType sceneType) {
        if (sceneType == null) {
            return null;
        }
        return switch (sceneType) {
            case HOME -> PortalHomeSceneType.HOME;
            case OFFICE_CENTER -> PortalHomeSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalHomeSceneType.MOBILE_WORKBENCH;
        };
    }

    private boolean transitionedToOrFromTenantWideWidget(PortalWidgetUpdatedEvent event) {
        return event.sceneType() == null
                || (event.previousCardType() != null
                && event.changedFields().contains("sceneType")
                && event.previousSceneType() == null);
    }

    private boolean sceneChangedBetweenSpecificScopes(PortalWidgetUpdatedEvent event) {
        return event.previousCardType() != null
                && event.changedFields().contains("sceneType")
                && event.previousSceneType() != null
                && event.previousSceneType() != event.sceneType();
    }
}
