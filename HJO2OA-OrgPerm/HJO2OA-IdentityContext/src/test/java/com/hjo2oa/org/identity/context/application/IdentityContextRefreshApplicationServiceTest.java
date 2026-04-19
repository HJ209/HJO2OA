package com.hjo2oa.org.identity.context.application;

import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidatedEvent;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidationReason;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.org.identity.context.infrastructure.InMemoryIdentityContextSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class IdentityContextRefreshApplicationServiceTest {

    @Test
    void shouldPublishIdentityContextInvalidatedEvent() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        IdentityContextSessionRepository sessionRepository = new InMemoryIdentityContextSessionRepository();
        IdentityContextRefreshApplicationService service = new IdentityContextRefreshApplicationService(
                publishedEvents::add,
                sessionRepository,
                Clock.fixed(Instant.parse("2026-04-19T12:00:00Z"), ZoneOffset.UTC)
        );

        RefreshIdentityContextResult result = service.refresh(new RefreshIdentityContextCommand(
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                "assignment-2",
                IdentityContextInvalidationReason.POSITION_DISABLED,
                false,
                42L,
                "org.position.disabled"
        ));

        assertEquals(1, publishedEvents.size());
        IdentityContextInvalidatedEvent event = assertInstanceOf(
                IdentityContextInvalidatedEvent.class,
                publishedEvents.get(0)
        );
        assertEquals("tenant-1", event.tenantId());
        assertEquals("person-1", event.personId());
        assertEquals("account-1", event.accountId());
        assertEquals("assignment-1", event.invalidatedAssignmentId());
        assertEquals("assignment-2", event.fallbackAssignmentId());
        assertEquals(IdentityContextInvalidationReason.POSITION_DISABLED, event.reasonCode());
        assertFalse(event.forceLogout());
        assertEquals(42L, event.permissionSnapshotVersion());
        assertEquals("org.position.disabled", event.triggerEvent());
        assertEquals(Instant.parse("2026-04-19T12:00:00Z"), event.occurredAt());
        assertEquals("assignment-2", result.currentContext().currentAssignmentId());
        assertEquals("position-2", result.currentContext().currentPositionId());
        assertEquals(42L, result.currentContext().permissionSnapshotVersion());
        assertEquals(event.eventId(), result.eventId());
        assertEquals(event.eventType(), result.eventType());
        assertEquals(event.occurredAt(), result.occurredAt());
        assertEquals(event.invalidatedAssignmentId(), result.invalidatedAssignmentId());
        assertEquals(event.fallbackAssignmentId(), result.fallbackAssignmentId());
        assertEquals(event.forceLogout(), result.forceLogout());
        assertEquals(event.permissionSnapshotVersion(), result.permissionSnapshotVersion());
    }
}
