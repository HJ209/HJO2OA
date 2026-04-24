package com.hjo2oa.org.identity.context.interfaces;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hjo2oa.org.identity.context.application.IdentityContextUpstreamInvalidationApplicationService;
import com.hjo2oa.org.identity.context.domain.OrgAccountLockedEvent;
import com.hjo2oa.org.identity.context.domain.OrgAssignmentExpiredEvent;
import com.hjo2oa.org.identity.context.domain.OrgAssignmentRemovedEvent;
import com.hjo2oa.org.identity.context.domain.OrgPersonDisabledEvent;
import com.hjo2oa.org.identity.context.domain.OrgPositionDisabledEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class IdentityContextUpstreamInvalidationEventListenerBindingTest {

    @Test
    void shouldDispatchPublishedEventsToUpstreamInvalidationApplicationService() {
        IdentityContextUpstreamInvalidationApplicationService invalidationApplicationService =
                mock(IdentityContextUpstreamInvalidationApplicationService.class);

        OrgAssignmentRemovedEvent assignmentRemovedEvent = new OrgAssignmentRemovedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T14:00:00Z"),
                "tenant-1",
                "person-1",
                "assignment-2",
                "position-2"
        );
        OrgAssignmentExpiredEvent assignmentExpiredEvent = new OrgAssignmentExpiredEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T14:01:00Z"),
                "tenant-1",
                "person-1",
                "assignment-2",
                "position-2"
        );
        OrgPositionDisabledEvent positionDisabledEvent = new OrgPositionDisabledEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T14:02:00Z"),
                "tenant-1",
                "position-1"
        );
        OrgPersonDisabledEvent personDisabledEvent = new OrgPersonDisabledEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T14:03:00Z"),
                "tenant-1",
                "person-1"
        );
        OrgAccountLockedEvent accountLockedEvent = new OrgAccountLockedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T14:04:00Z"),
                "tenant-1",
                "account-1",
                "person-1"
        );

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(
                    IdentityContextUpstreamInvalidationApplicationService.class,
                    () -> invalidationApplicationService
            );
            context.registerBean(IdentityContextUpstreamInvalidationEventListener.class);
            context.refresh();

            context.publishEvent(assignmentRemovedEvent);
            context.publishEvent(assignmentExpiredEvent);
            context.publishEvent(positionDisabledEvent);
            context.publishEvent(personDisabledEvent);
            context.publishEvent(accountLockedEvent);
        }

        verify(invalidationApplicationService).onAssignmentRemoved(assignmentRemovedEvent);
        verify(invalidationApplicationService).onAssignmentExpired(assignmentExpiredEvent);
        verify(invalidationApplicationService).onPositionDisabled(positionDisabledEvent);
        verify(invalidationApplicationService).onPersonDisabled(personDisabledEvent);
        verify(invalidationApplicationService).onAccountLocked(accountLockedEvent);
    }
}
