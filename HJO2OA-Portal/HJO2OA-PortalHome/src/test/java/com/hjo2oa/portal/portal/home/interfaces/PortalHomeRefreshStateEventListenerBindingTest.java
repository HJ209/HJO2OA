package com.hjo2oa.portal.portal.home.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.hjo2oa.org.identity.context.domain.IdentityAssignmentType;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidatedEvent;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidationReason;
import com.hjo2oa.org.identity.context.domain.IdentitySwitchedEvent;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotFailedEvent;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotRefreshedEvent;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationSavedEvent;
import com.hjo2oa.portal.portal.home.application.PortalHomeRefreshStateApplicationService;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class PortalHomeRefreshStateEventListenerBindingTest {

    @Test
    void shouldDispatchPublishedEventsToRefreshStateApplicationService() {
        PortalHomeRefreshStateApplicationService refreshStateApplicationService =
                mock(PortalHomeRefreshStateApplicationService.class);

        IdentitySwitchedEvent identitySwitchedEvent = new IdentitySwitchedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:00:00Z"),
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                "assignment-2",
                "position-1",
                "position-2",
                IdentityAssignmentType.PRIMARY,
                IdentityAssignmentType.SECONDARY,
                "manual-switch"
        );
        IdentityContextInvalidatedEvent invalidatedEvent = new IdentityContextInvalidatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:01:00Z"),
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                "assignment-2",
                IdentityContextInvalidationReason.PRIMARY_CHANGED,
                false,
                2L,
                "org.assignment.changed"
        );
        PortalSnapshotRefreshedEvent snapshotRefreshedEvent = new PortalSnapshotRefreshedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:02:00Z"),
                "tenant-1",
                "person-1",
                "assignment-1",
                "portal:agg:tenant-1:person-1:assignment-1:position-1:HOME:TODO",
                PortalSceneType.HOME,
                PortalCardType.TODO,
                Instant.parse("2026-04-19T12:02:00Z")
        );
        PortalSnapshotFailedEvent snapshotFailedEvent = new PortalSnapshotFailedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:03:00Z"),
                "tenant-1",
                "person-1",
                "assignment-1",
                "portal:agg:tenant-1:person-1:assignment-1:position-1:HOME:MESSAGE",
                PortalSceneType.HOME,
                PortalCardType.MESSAGE,
                "Message card is temporarily unavailable"
        );
        PortalPersonalizationSavedEvent personalizationSavedEvent = new PortalPersonalizationSavedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:04:00Z"),
                "tenant-1",
                "profile-1",
                "person-1",
                PersonalizationSceneType.OFFICE_CENTER
        );
        PortalWidgetDisabledEvent widgetDisabledEvent = new PortalWidgetDisabledEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:05:00Z"),
                "tenant-1",
                "widget-2",
                "message-card",
                WidgetCardType.MESSAGE,
                null
        );

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(
                    PortalHomeRefreshStateApplicationService.class,
                    () -> refreshStateApplicationService
            );
            context.registerBean(PortalHomeRefreshStateEventListener.class);
            context.refresh();

            context.publishEvent(identitySwitchedEvent);
            context.publishEvent(invalidatedEvent);
            context.publishEvent(snapshotRefreshedEvent);
            context.publishEvent(snapshotFailedEvent);
            context.publishEvent(personalizationSavedEvent);
            context.publishEvent(widgetDisabledEvent);
        }

        verify(refreshStateApplicationService, times(2))
                .markReloadRequiredAllScenesForPerson(eq("tenant-1"), eq("person-1"), anyString(), any(Instant.class));
        verify(refreshStateApplicationService)
                .markReloadRequiredAllScenesForTenant(eq("tenant-1"), anyString(), any(Instant.class));
        verify(refreshStateApplicationService)
                .markCardRefreshed(
                        eq("tenant-1"),
                        eq("person-1"),
                        eq("assignment-1"),
                        eq(PortalHomeSceneType.HOME),
                        eq(PortalCardType.TODO),
                        eq(PortalSnapshotRefreshedEvent.EVENT_TYPE),
                        any(Instant.class)
                );
        verify(refreshStateApplicationService)
                .markCardFailed(
                        eq("tenant-1"),
                        eq("person-1"),
                        eq("assignment-1"),
                        eq(PortalHomeSceneType.HOME),
                        eq(PortalCardType.MESSAGE),
                        eq(PortalSnapshotFailedEvent.EVENT_TYPE),
                        eq("Message card is temporarily unavailable"),
                        any(Instant.class)
                );
        verify(refreshStateApplicationService)
                .markReloadRequiredForPerson(
                        eq("tenant-1"),
                        eq("person-1"),
                        eq(PortalHomeSceneType.OFFICE_CENTER),
                        eq(PortalPersonalizationSavedEvent.EVENT_TYPE),
                        any(Instant.class)
                );
    }
}
