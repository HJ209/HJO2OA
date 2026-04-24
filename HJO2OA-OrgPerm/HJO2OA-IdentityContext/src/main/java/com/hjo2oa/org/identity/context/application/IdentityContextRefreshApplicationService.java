package com.hjo2oa.org.identity.context.application;

import com.hjo2oa.org.identity.context.domain.IdentityAssignment;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidatedEvent;
import com.hjo2oa.org.identity.context.domain.IdentityContextSession;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import com.hjo2oa.org.identity.context.domain.IdentityContextView;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IdentityContextRefreshApplicationService {

    private final DomainEventPublisher domainEventPublisher;
    private final IdentityContextSessionRepository sessionRepository;
    private final Clock clock;

    public IdentityContextRefreshApplicationService(
            DomainEventPublisher domainEventPublisher,
            IdentityContextSessionRepository sessionRepository
    ) {
        this(domainEventPublisher, sessionRepository, Clock.systemUTC());
    }

    IdentityContextRefreshApplicationService(
            DomainEventPublisher domainEventPublisher,
            IdentityContextSessionRepository sessionRepository,
            Clock clock
    ) {
        this.domainEventPublisher = domainEventPublisher;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    public RefreshIdentityContextResult refresh(RefreshIdentityContextCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        IdentityContextSession session = sessionRepository.currentSession();
        Instant occurredAt = Instant.now(clock);
        long nextVersion = command.permissionSnapshotVersion() > 0
                ? command.permissionSnapshotVersion()
                : session.permissionSnapshotVersion() + 1;

        RefreshIdentityContextOutcome outcome = RefreshIdentityContextOutcome.RELOGIN_REQUIRED;
        IdentityContextView currentContext = null;
        if (!command.forceLogout()) {
            RefreshSessionDecision decision = refreshSession(session, command, nextVersion, occurredAt);
            sessionRepository.save(decision.session());
            outcome = decision.outcome();
            currentContext = decision.session().currentContext();
        }

        IdentityContextInvalidatedEvent event = new IdentityContextInvalidatedEvent(
                UUID.randomUUID(),
                occurredAt,
                command.tenantId(),
                command.personId(),
                command.accountId(),
                command.invalidatedAssignmentId(),
                command.fallbackAssignmentId(),
                command.reasonCode(),
                command.forceLogout(),
                nextVersion,
                command.triggerEvent()
        );
        domainEventPublisher.publish(event);
        return new RefreshIdentityContextResult(
                event.eventId(),
                event.eventType(),
                event.occurredAt(),
                event.invalidatedAssignmentId(),
                event.fallbackAssignmentId(),
                event.forceLogout(),
                event.permissionSnapshotVersion(),
                outcome,
                currentContext
        );
    }

    private RefreshSessionDecision refreshSession(
            IdentityContextSession session,
            RefreshIdentityContextCommand command,
            long nextVersion,
            Instant occurredAt
    ) {
        if (command.fallbackAssignmentId() != null) {
            IdentityAssignment fallbackAssignment = session.findByAssignmentId(command.fallbackAssignmentId())
                    .filter(IdentityAssignment::active)
                    .orElse(null);
            if (fallbackAssignment != null) {
                IdentityContextSession refreshedSession = session.withCurrentAssignment(
                        fallbackAssignment.assignmentId(),
                        nextVersion,
                        occurredAt
                );
                RefreshIdentityContextOutcome outcome = fallbackAssignment.isPrimary()
                        ? RefreshIdentityContextOutcome.FALLBACK_TO_PRIMARY
                        : RefreshIdentityContextOutcome.RECOVERED;
                return new RefreshSessionDecision(refreshedSession, outcome);
            }
        }

        if (session.currentAssignmentId().equals(command.invalidatedAssignmentId())) {
            IdentityAssignment primaryAssignment = session.primaryAssignment()
                    .filter(IdentityAssignment::active)
                    .orElse(null);
            if (primaryAssignment != null && !primaryAssignment.assignmentId().equals(command.invalidatedAssignmentId())) {
                return new RefreshSessionDecision(
                        session.withCurrentAssignment(primaryAssignment.assignmentId(), nextVersion, occurredAt),
                        RefreshIdentityContextOutcome.FALLBACK_TO_PRIMARY
                );
            }
        }

        return new RefreshSessionDecision(
                session.withPermissionSnapshotVersion(nextVersion, occurredAt),
                RefreshIdentityContextOutcome.RECOVERED
        );
    }

    private record RefreshSessionDecision(
            IdentityContextSession session,
            RefreshIdentityContextOutcome outcome
    ) {
    }
}
