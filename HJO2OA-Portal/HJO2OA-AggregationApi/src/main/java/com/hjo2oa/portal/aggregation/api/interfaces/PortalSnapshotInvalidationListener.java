package com.hjo2oa.portal.aggregation.api.interfaces;

import com.hjo2oa.msg.message.center.domain.MsgNotificationReadEvent;
import com.hjo2oa.msg.message.center.domain.MsgNotificationSentEvent;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidatedEvent;
import com.hjo2oa.org.identity.context.domain.IdentitySwitchedEvent;
import com.hjo2oa.portal.aggregation.api.application.PortalSnapshotInvalidationApplicationService;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotScope;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationResetEvent;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationSavedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.todo.center.domain.TodoItemCreatedEvent;
import com.hjo2oa.todo.center.domain.TodoItemOverdueEvent;
import java.util.EnumSet;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PortalSnapshotInvalidationListener {

    private final PortalSnapshotInvalidationApplicationService invalidationApplicationService;

    public PortalSnapshotInvalidationListener(
            PortalSnapshotInvalidationApplicationService invalidationApplicationService
    ) {
        this.invalidationApplicationService = invalidationApplicationService;
    }

    @EventListener
    public void onIdentitySwitched(IdentitySwitchedEvent event) {
        markIdentitySnapshotsStale(
                event.tenantId(),
                event.personId(),
                event.fromAssignmentId(),
                event.fromPositionId(),
                event.eventType()
        );
        if (sameIdentitySession(
                event.fromAssignmentId(),
                event.fromPositionId(),
                event.toAssignmentId(),
                event.toPositionId()
        )) {
            return;
        }
        markIdentitySnapshotsStale(
                event.tenantId(),
                event.personId(),
                event.toAssignmentId(),
                event.toPositionId(),
                event.eventType()
        );
    }

    @EventListener
    public void onIdentityContextInvalidated(IdentityContextInvalidatedEvent event) {
        if (event.forceLogout() || event.fallbackAssignmentId() == null) {
            invalidationApplicationService.markStale(
                    PortalSnapshotScope.ofIdentity(
                            event.tenantId(),
                            event.personId(),
                            null,
                            null
                    ),
                    EnumSet.allOf(PortalCardType.class),
                    event.eventType()
            );
            return;
        }
        markIdentitySnapshotsStale(
                event.tenantId(),
                event.personId(),
                event.invalidatedAssignmentId(),
                null,
                event.eventType()
        );
        if (Objects.equals(event.invalidatedAssignmentId(), event.fallbackAssignmentId())) {
            return;
        }
        markIdentitySnapshotsStale(
                event.tenantId(),
                event.personId(),
                event.fallbackAssignmentId(),
                null,
                event.eventType()
        );
    }

    @EventListener
    public void onTodoItemCreated(TodoItemCreatedEvent event) {
        invalidationApplicationService.markStale(
                PortalSnapshotScope.ofAssignment(event.tenantId(), event.assigneeId()),
                EnumSet.of(PortalCardType.TODO),
                event.eventType()
        );
    }

    @EventListener
    public void onTodoItemOverdue(TodoItemOverdueEvent event) {
        invalidationApplicationService.markStale(
                PortalSnapshotScope.ofAssignment(event.tenantId(), event.assigneeId()),
                EnumSet.of(PortalCardType.TODO),
                event.eventType()
        );
    }

    @EventListener
    public void onNotificationSent(MsgNotificationSentEvent event) {
        invalidationApplicationService.markStale(
                PortalSnapshotScope.ofAssignment(event.tenantId(), event.recipientId()),
                EnumSet.of(PortalCardType.MESSAGE),
                event.eventType()
        );
    }

    @EventListener
    public void onNotificationRead(MsgNotificationReadEvent event) {
        invalidationApplicationService.markStale(
                PortalSnapshotScope.ofAssignment(event.tenantId(), event.recipientId()),
                EnumSet.of(PortalCardType.MESSAGE),
                event.eventType()
        );
    }

    @EventListener
    public void onPublicationActivated(PortalPublicationActivatedEvent event) {
        invalidationApplicationService.markStale(
                PortalSnapshotScope.ofScene(event.tenantId(), mapPublicationSceneType(event.sceneType())),
                EnumSet.allOf(PortalCardType.class),
                event.eventType()
        );
    }

    @EventListener
    public void onPublicationOfflined(PortalPublicationOfflinedEvent event) {
        invalidationApplicationService.markStale(
                tenantSceneScope(event.tenantId(), mapPublicationSceneType(event.sceneType())),
                EnumSet.allOf(PortalCardType.class),
                event.eventType()
        );
    }

    @EventListener
    public void onPersonalizationSaved(PortalPersonalizationSavedEvent event) {
        invalidationApplicationService.markStale(
                PortalSnapshotScope.ofPersonScene(event.tenantId(), event.personId(), mapSceneType(event.sceneType())),
                EnumSet.allOf(PortalCardType.class),
                event.eventType()
        );
    }

    @EventListener
    public void onPersonalizationReset(PortalPersonalizationResetEvent event) {
        invalidationApplicationService.markStale(
                PortalSnapshotScope.ofPersonScene(event.tenantId(), event.personId(), mapSceneType(event.sceneType())),
                EnumSet.allOf(PortalCardType.class),
                event.eventType()
        );
    }

    @EventListener
    public void onWidgetUpdated(PortalWidgetUpdatedEvent event) {
        markWidgetSnapshotsStale(
                event.tenantId(),
                event.sceneType(),
                event.cardType(),
                event.eventType()
        );
        if (hasPreviousWidgetTarget(event)) {
            markWidgetSnapshotsStale(
                    event.tenantId(),
                    event.previousSceneType(),
                    event.previousCardType(),
                    event.eventType()
            );
        }
    }

    @EventListener
    public void onWidgetDisabled(PortalWidgetDisabledEvent event) {
        invalidationApplicationService.markStale(
                tenantSceneScope(event.tenantId(), mapWidgetSceneType(event.sceneType())),
                EnumSet.of(mapWidgetCardType(event.cardType())),
                event.eventType()
        );
    }

    private PortalSceneType mapPublicationSceneType(PortalPublicationSceneType sceneType) {
        if (sceneType == null) {
            return null;
        }
        return switch (sceneType) {
            case HOME -> PortalSceneType.HOME;
            case OFFICE_CENTER -> PortalSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalSceneType.MOBILE_WORKBENCH;
        };
    }

    private PortalSnapshotScope tenantSceneScope(String tenantId, PortalSceneType sceneType) {
        return sceneType == null
                ? PortalSnapshotScope.ofTenant(tenantId)
                : PortalSnapshotScope.ofScene(tenantId, sceneType);
    }

    private PortalSceneType mapSceneType(PersonalizationSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> PortalSceneType.HOME;
            case OFFICE_CENTER -> PortalSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalSceneType.MOBILE_WORKBENCH;
        };
    }

    private PortalCardType mapWidgetCardType(WidgetCardType cardType) {
        return switch (cardType) {
            case IDENTITY -> PortalCardType.IDENTITY;
            case TODO -> PortalCardType.TODO;
            case MESSAGE -> PortalCardType.MESSAGE;
        };
    }

    private PortalSceneType mapWidgetSceneType(WidgetSceneType sceneType) {
        if (sceneType == null) {
            return null;
        }
        return switch (sceneType) {
            case HOME -> PortalSceneType.HOME;
            case OFFICE_CENTER -> PortalSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> PortalSceneType.MOBILE_WORKBENCH;
        };
    }

    private void markWidgetSnapshotsStale(
            String tenantId,
            WidgetSceneType sceneType,
            WidgetCardType cardType,
            String reason
    ) {
        invalidationApplicationService.markStale(
                tenantSceneScope(tenantId, mapWidgetSceneType(sceneType)),
                EnumSet.of(mapWidgetCardType(cardType)),
                reason
        );
    }

    private boolean hasPreviousWidgetTarget(PortalWidgetUpdatedEvent event) {
        return event.previousCardType() != null
                && (event.previousCardType() != event.cardType()
                || event.previousSceneType() != event.sceneType());
    }

    private void markIdentitySnapshotsStale(
            String tenantId,
            String personId,
            String assignmentId,
            String positionId,
            String reason
    ) {
        invalidationApplicationService.markStale(
                PortalSnapshotScope.ofIdentity(tenantId, personId, assignmentId, positionId),
                EnumSet.allOf(PortalCardType.class),
                reason
        );
    }

    private boolean sameIdentitySession(
            String firstAssignmentId,
            String firstPositionId,
            String secondAssignmentId,
            String secondPositionId
    ) {
        return Objects.equals(firstAssignmentId, secondAssignmentId)
                && Objects.equals(firstPositionId, secondPositionId);
    }
}
