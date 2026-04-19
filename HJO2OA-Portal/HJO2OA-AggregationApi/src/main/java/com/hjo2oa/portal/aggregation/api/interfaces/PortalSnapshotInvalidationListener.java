package com.hjo2oa.portal.aggregation.api.interfaces;

import com.hjo2oa.msg.message.center.domain.MsgNotificationReadEvent;
import com.hjo2oa.msg.message.center.domain.MsgNotificationSentEvent;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidatedEvent;
import com.hjo2oa.org.identity.context.domain.IdentitySwitchedEvent;
import com.hjo2oa.portal.aggregation.api.application.PortalSnapshotInvalidationApplicationService;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotScope;
import com.hjo2oa.todo.center.domain.TodoItemCreatedEvent;
import com.hjo2oa.todo.center.domain.TodoItemOverdueEvent;
import java.util.EnumSet;
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
    }

    @EventListener
    public void onIdentityContextInvalidated(IdentityContextInvalidatedEvent event) {
        invalidationApplicationService.markStale(
                PortalSnapshotScope.ofIdentity(
                        event.tenantId(),
                        event.personId(),
                        event.invalidatedAssignmentId(),
                        null
                ),
                EnumSet.allOf(PortalCardType.class),
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
}
