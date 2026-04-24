package com.hjo2oa.org.identity.context.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidatedEvent;
import com.hjo2oa.org.identity.context.domain.IdentityContextSession;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import com.hjo2oa.org.identity.context.domain.OrgAccountLockedEvent;
import com.hjo2oa.org.identity.context.domain.OrgAssignmentExpiredEvent;
import com.hjo2oa.org.identity.context.domain.OrgPositionDisabledEvent;
import com.hjo2oa.org.identity.context.infrastructure.InMemoryIdentityContextSessionRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityContextUpstreamInvalidationApplicationServiceTest {

    @Test
    void shouldFallbackToPrimaryWhenCurrentSecondaryAssignmentExpires() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        IdentityContextSession seededSession = new InMemoryIdentityContextSessionRepository().currentSession()
                .withCurrentAssignment("assignment-2", 3L, Instant.parse("2026-04-19T08:00:00Z"));
        IdentityContextSessionRepository sessionRepository = new InMemoryIdentityContextSessionRepository(seededSession);
        IdentityContextUpstreamInvalidationApplicationService service = new IdentityContextUpstreamInvalidationApplicationService(
                sessionRepository,
                new IdentityContextRefreshApplicationService(
                        publishedEvents::add,
                        sessionRepository,
                        Clock.fixed(Instant.parse("2026-04-19T12:00:00Z"), ZoneOffset.UTC)
                )
        );

        Optional<RefreshIdentityContextResult> result = service.onAssignmentExpired(new OrgAssignmentExpiredEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T11:55:00Z"),
                "tenant-1",
                "person-1",
                "assignment-2",
                "position-2"
        ));

        assertTrue(result.isPresent());
        assertSame(RefreshIdentityContextOutcome.FALLBACK_TO_PRIMARY, result.get().outcome());
        assertEquals("assignment-1", result.get().currentContext().currentAssignmentId());
        assertEquals("assignment-1", sessionRepository.currentSession().currentAssignmentId());
        IdentityContextInvalidatedEvent invalidatedEvent = assertInstanceOf(
                IdentityContextInvalidatedEvent.class,
                publishedEvents.get(0)
        );
        assertEquals("org.assignment.expired", invalidatedEvent.triggerEvent());
        assertFalse(invalidatedEvent.forceLogout());
    }

    @Test
    void shouldForceLogoutWhenAccountIsLocked() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        IdentityContextSessionRepository sessionRepository = new InMemoryIdentityContextSessionRepository();
        IdentityContextSession originalSession = sessionRepository.currentSession();
        IdentityContextUpstreamInvalidationApplicationService service = new IdentityContextUpstreamInvalidationApplicationService(
                sessionRepository,
                new IdentityContextRefreshApplicationService(
                        publishedEvents::add,
                        sessionRepository,
                        Clock.fixed(Instant.parse("2026-04-19T12:30:00Z"), ZoneOffset.UTC)
                )
        );

        Optional<RefreshIdentityContextResult> result = service.onAccountLocked(new OrgAccountLockedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:29:00Z"),
                "tenant-1",
                "account-1",
                "person-1"
        ));

        assertTrue(result.isPresent());
        assertSame(RefreshIdentityContextOutcome.RELOGIN_REQUIRED, result.get().outcome());
        assertNull(result.get().currentContext());
        assertEquals(originalSession, sessionRepository.currentSession());
        IdentityContextInvalidatedEvent invalidatedEvent = assertInstanceOf(
                IdentityContextInvalidatedEvent.class,
                publishedEvents.get(0)
        );
        assertTrue(invalidatedEvent.forceLogout());
        assertEquals("org.account.locked", invalidatedEvent.triggerEvent());
    }

    @Test
    void shouldIgnoreUnrelatedPositionDisabledEvent() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        IdentityContextSessionRepository sessionRepository = new InMemoryIdentityContextSessionRepository();
        IdentityContextUpstreamInvalidationApplicationService service = new IdentityContextUpstreamInvalidationApplicationService(
                sessionRepository,
                new IdentityContextRefreshApplicationService(
                        publishedEvents::add,
                        sessionRepository,
                        Clock.fixed(Instant.parse("2026-04-19T13:00:00Z"), ZoneOffset.UTC)
                )
        );

        Optional<RefreshIdentityContextResult> result = service.onPositionDisabled(new OrgPositionDisabledEvent(
                UUID.randomUUID(),
                Instant.parse("2026-04-19T12:59:00Z"),
                "tenant-1",
                "position-9"
        ));

        assertTrue(result.isEmpty());
        assertTrue(publishedEvents.isEmpty());
        assertEquals("assignment-1", sessionRepository.currentSession().currentAssignmentId());
    }
}
