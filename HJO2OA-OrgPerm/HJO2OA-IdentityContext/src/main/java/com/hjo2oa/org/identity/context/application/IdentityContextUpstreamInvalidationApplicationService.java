package com.hjo2oa.org.identity.context.application;

import com.hjo2oa.org.identity.context.domain.IdentityAssignment;
import com.hjo2oa.org.identity.context.domain.IdentityContextInvalidationReason;
import com.hjo2oa.org.identity.context.domain.IdentityContextSession;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import com.hjo2oa.org.identity.context.domain.OrgAccountLockedEvent;
import com.hjo2oa.org.identity.context.domain.OrgAssignmentExpiredEvent;
import com.hjo2oa.org.identity.context.domain.OrgAssignmentRemovedEvent;
import com.hjo2oa.org.identity.context.domain.OrgPersonDisabledEvent;
import com.hjo2oa.org.identity.context.domain.OrgPositionDisabledEvent;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class IdentityContextUpstreamInvalidationApplicationService {

    private final IdentityContextSessionRepository sessionRepository;
    private final IdentityContextRefreshApplicationService refreshApplicationService;

    public IdentityContextUpstreamInvalidationApplicationService(
            IdentityContextSessionRepository sessionRepository,
            IdentityContextRefreshApplicationService refreshApplicationService
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.refreshApplicationService = Objects.requireNonNull(
                refreshApplicationService,
                "refreshApplicationService must not be null"
        );
    }

    public Optional<RefreshIdentityContextResult> onAssignmentRemoved(OrgAssignmentRemovedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        IdentityContextSession session = sessionRepository.currentSession();
        if (!sameTenantAndPerson(session, event.tenantId(), event.personId())
                || !session.currentAssignmentId().equals(event.assignmentId())) {
            return Optional.empty();
        }
        return Optional.of(refreshCurrentAssignment(
                session,
                event.assignmentId(),
                IdentityContextInvalidationReason.ASSIGNMENT_REMOVED,
                event.eventType()
        ));
    }

    public Optional<RefreshIdentityContextResult> onAssignmentExpired(OrgAssignmentExpiredEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        IdentityContextSession session = sessionRepository.currentSession();
        if (!sameTenantAndPerson(session, event.tenantId(), event.personId())
                || !session.currentAssignmentId().equals(event.assignmentId())) {
            return Optional.empty();
        }
        return Optional.of(refreshCurrentAssignment(
                session,
                event.assignmentId(),
                IdentityContextInvalidationReason.ASSIGNMENT_EXPIRED,
                event.eventType()
        ));
    }

    public Optional<RefreshIdentityContextResult> onPositionDisabled(OrgPositionDisabledEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        IdentityContextSession session = sessionRepository.currentSession();
        if (!session.tenantId().equals(event.tenantId())) {
            return Optional.empty();
        }
        String currentPositionId = session.currentContext().currentPositionId();
        if (!currentPositionId.equals(event.positionId())) {
            return Optional.empty();
        }
        return Optional.of(refreshCurrentAssignment(
                session,
                session.currentAssignmentId(),
                IdentityContextInvalidationReason.POSITION_DISABLED,
                event.eventType()
        ));
    }

    public Optional<RefreshIdentityContextResult> onPersonDisabled(OrgPersonDisabledEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        IdentityContextSession session = sessionRepository.currentSession();
        if (!sameTenantAndPerson(session, event.tenantId(), event.personId())) {
            return Optional.empty();
        }
        return Optional.of(forceLogout(
                session,
                IdentityContextInvalidationReason.PERSON_DISABLED,
                event.eventType()
        ));
    }

    public Optional<RefreshIdentityContextResult> onAccountLocked(OrgAccountLockedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        IdentityContextSession session = sessionRepository.currentSession();
        if (!session.tenantId().equals(event.tenantId())
                || !session.accountId().equals(event.accountId())
                || !session.personId().equals(event.personId())) {
            return Optional.empty();
        }
        return Optional.of(forceLogout(
                session,
                IdentityContextInvalidationReason.ACCOUNT_LOCKED,
                event.eventType()
        ));
    }

    private RefreshIdentityContextResult refreshCurrentAssignment(
            IdentityContextSession session,
            String invalidatedAssignmentId,
            IdentityContextInvalidationReason reason,
            String triggerEvent
    ) {
        String fallbackAssignmentId = session.primaryAssignment()
                .filter(IdentityAssignment::active)
                .map(IdentityAssignment::assignmentId)
                .filter(candidate -> !candidate.equals(invalidatedAssignmentId))
                .orElse(null);
        boolean forceLogout = fallbackAssignmentId == null;
        return refreshApplicationService.refresh(new RefreshIdentityContextCommand(
                session.tenantId(),
                session.personId(),
                session.accountId(),
                invalidatedAssignmentId,
                fallbackAssignmentId,
                reason,
                forceLogout,
                0L,
                triggerEvent
        ));
    }

    private RefreshIdentityContextResult forceLogout(
            IdentityContextSession session,
            IdentityContextInvalidationReason reason,
            String triggerEvent
    ) {
        return refreshApplicationService.refresh(new RefreshIdentityContextCommand(
                session.tenantId(),
                session.personId(),
                session.accountId(),
                session.currentAssignmentId(),
                null,
                reason,
                true,
                0L,
                triggerEvent
        ));
    }

    private boolean sameTenantAndPerson(IdentityContextSession session, String tenantId, String personId) {
        return session.tenantId().equals(tenantId) && session.personId().equals(personId);
    }
}
