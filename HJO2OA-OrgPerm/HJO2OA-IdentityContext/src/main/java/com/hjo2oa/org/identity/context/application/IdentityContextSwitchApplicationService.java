package com.hjo2oa.org.identity.context.application;

import com.hjo2oa.org.identity.context.domain.IdentityAssignment;
import com.hjo2oa.org.identity.context.domain.IdentityAssignmentType;
import com.hjo2oa.org.identity.context.domain.IdentityContextSession;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import com.hjo2oa.org.identity.context.domain.IdentityContextView;
import com.hjo2oa.org.identity.context.domain.IdentitySwitchedEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IdentityContextSwitchApplicationService {

    private final IdentityContextSessionRepository sessionRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public IdentityContextSwitchApplicationService(
            IdentityContextSessionRepository sessionRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(sessionRepository, domainEventPublisher, Clock.systemUTC());
    }

    IdentityContextSwitchApplicationService(
            IdentityContextSessionRepository sessionRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    public IdentityContextView switchToSecondary(SwitchIdentityContextCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        IdentityContextSession session = sessionRepository.currentSession();
        IdentityContextView currentContext = session.currentContext();
        IdentityAssignment targetAssignment = session.findByPositionId(requireText(command.targetPositionId(), "targetPositionId"))
                .orElseThrow(() -> IdentityContextOperationException.forbidden(
                        "IDENTITY_SWITCH_FORBIDDEN",
                        "Target position is not a valid secondary assignment"
                ));

        if (targetAssignment.assignmentType() != IdentityAssignmentType.SECONDARY) {
            throw IdentityContextOperationException.forbidden(
                    "IDENTITY_SWITCH_FORBIDDEN",
                    "Target position is not a valid secondary assignment"
            );
        }

        if (!targetAssignment.active()) {
            throw IdentityContextOperationException.conflict(
                    "IDENTITY_SWITCH_TARGET_INACTIVE",
                    "Target assignment is inactive"
            );
        }

        if (currentContext.currentPositionId().equals(targetAssignment.positionId())) {
            return currentContext;
        }

        Instant occurredAt = Instant.now(clock);
        IdentityContextSession updatedSession = session.withCurrentAssignment(
                targetAssignment.assignmentId(),
                session.permissionSnapshotVersion() + 1,
                occurredAt
        );
        sessionRepository.save(updatedSession);

        IdentityContextView newContext = updatedSession.currentContext();
        domainEventPublisher.publish(new IdentitySwitchedEvent(
                UUID.randomUUID(),
                occurredAt,
                session.tenantId(),
                session.personId(),
                session.accountId(),
                currentContext.currentAssignmentId(),
                newContext.currentAssignmentId(),
                currentContext.currentPositionId(),
                newContext.currentPositionId(),
                currentContext.assignmentType(),
                newContext.assignmentType(),
                command.reason()
        ));

        return newContext;
    }

    public IdentityContextView resetPrimary(ResetPrimaryIdentityContextCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        IdentityContextSession session = sessionRepository.currentSession();
        IdentityContextView currentContext = session.currentContext();
        IdentityAssignment primaryAssignment = session.primaryAssignment()
                .orElseThrow(() -> IdentityContextOperationException.conflict(
                        "PRIMARY_ASSIGNMENT_MISSING",
                        "Primary assignment is missing"
                ));

        if (!primaryAssignment.active()) {
            throw IdentityContextOperationException.conflict(
                    "PRIMARY_ASSIGNMENT_MISSING",
                    "Primary assignment is missing"
            );
        }

        if (currentContext.currentAssignmentId().equals(primaryAssignment.assignmentId())) {
            return currentContext;
        }

        Instant occurredAt = Instant.now(clock);
        IdentityContextSession updatedSession = session.withCurrentAssignment(
                primaryAssignment.assignmentId(),
                session.permissionSnapshotVersion() + 1,
                occurredAt
        );
        sessionRepository.save(updatedSession);

        IdentityContextView newContext = updatedSession.currentContext();
        domainEventPublisher.publish(new IdentitySwitchedEvent(
                UUID.randomUUID(),
                occurredAt,
                session.tenantId(),
                session.personId(),
                session.accountId(),
                currentContext.currentAssignmentId(),
                newContext.currentAssignmentId(),
                currentContext.currentPositionId(),
                newContext.currentPositionId(),
                currentContext.assignmentType(),
                newContext.assignmentType(),
                command.reason()
        ));

        return newContext;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
