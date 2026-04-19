package com.hjo2oa.org.identity.context.application;

import com.hjo2oa.org.identity.context.domain.IdentityContextView;
import com.hjo2oa.org.identity.context.domain.IdentitySwitchedEvent;
import com.hjo2oa.org.identity.context.infrastructure.InMemoryIdentityContextSessionRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentityContextSwitchApplicationServiceTest {

    @Test
    void shouldSwitchToSecondaryAssignmentAndPublishEvent() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        IdentityContextSwitchApplicationService service = new IdentityContextSwitchApplicationService(
                new InMemoryIdentityContextSessionRepository(),
                publishedEvents::add,
                Clock.fixed(Instant.parse("2026-04-19T15:00:00Z"), ZoneOffset.UTC)
        );

        IdentityContextView currentContext = service.switchToSecondary(new SwitchIdentityContextCommand(
                "position-2",
                "manual-switch"
        ));

        assertEquals("assignment-2", currentContext.currentAssignmentId());
        assertEquals("position-2", currentContext.currentPositionId());
        assertEquals(2L, currentContext.permissionSnapshotVersion());
        assertEquals(Instant.parse("2026-04-19T15:00:00Z"), currentContext.effectiveAt());
        assertEquals(1, publishedEvents.size());
        IdentitySwitchedEvent event = assertInstanceOf(IdentitySwitchedEvent.class, publishedEvents.get(0));
        assertEquals("assignment-1", event.fromAssignmentId());
        assertEquals("assignment-2", event.toAssignmentId());
        assertEquals("position-1", event.fromPositionId());
        assertEquals("position-2", event.toPositionId());
        assertEquals("manual-switch", event.reason());
    }

    @Test
    void shouldRejectSwitchToUnknownSecondaryAssignment() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        IdentityContextSwitchApplicationService service = new IdentityContextSwitchApplicationService(
                new InMemoryIdentityContextSessionRepository(),
                publishedEvents::add,
                Clock.fixed(Instant.parse("2026-04-19T15:00:00Z"), ZoneOffset.UTC)
        );

        IdentityContextOperationException ex = assertThrows(
                IdentityContextOperationException.class,
                () -> service.switchToSecondary(new SwitchIdentityContextCommand("position-unknown", "manual-switch"))
        );

        assertEquals("IDENTITY_SWITCH_FORBIDDEN", ex.errorCode());
        assertEquals(0, publishedEvents.size());
    }

    @Test
    void shouldRejectInactiveSecondaryAssignment() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        IdentityContextSwitchApplicationService service = new IdentityContextSwitchApplicationService(
                new InMemoryIdentityContextSessionRepository(),
                publishedEvents::add,
                Clock.fixed(Instant.parse("2026-04-19T15:00:00Z"), ZoneOffset.UTC)
        );

        IdentityContextOperationException ex = assertThrows(
                IdentityContextOperationException.class,
                () -> service.switchToSecondary(new SwitchIdentityContextCommand("position-3", "manual-switch"))
        );

        assertEquals("IDENTITY_SWITCH_TARGET_INACTIVE", ex.errorCode());
        assertEquals(0, publishedEvents.size());
    }

    @Test
    void shouldResetToPrimaryAssignment() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        IdentityContextSwitchApplicationService service = new IdentityContextSwitchApplicationService(
                new InMemoryIdentityContextSessionRepository(),
                publishedEvents::add,
                Clock.fixed(Instant.parse("2026-04-19T15:00:00Z"), ZoneOffset.UTC)
        );

        service.switchToSecondary(new SwitchIdentityContextCommand("position-2", "manual-switch"));
        IdentityContextView currentContext = service.resetPrimary(new ResetPrimaryIdentityContextCommand(null));

        assertEquals("assignment-1", currentContext.currentAssignmentId());
        assertEquals("position-1", currentContext.currentPositionId());
        assertEquals(3L, currentContext.permissionSnapshotVersion());
        assertEquals(2, publishedEvents.size());
        IdentitySwitchedEvent event = assertInstanceOf(IdentitySwitchedEvent.class, publishedEvents.get(1));
        assertEquals("assignment-2", event.fromAssignmentId());
        assertEquals("assignment-1", event.toAssignmentId());
        assertNull(event.reason());
    }
}
