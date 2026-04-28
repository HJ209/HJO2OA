package com.hjo2oa.org.identity.context.application;

import com.hjo2oa.org.identity.context.domain.IdentityAssignment;
import com.hjo2oa.org.identity.context.domain.IdentityAssignmentType;
import com.hjo2oa.org.identity.context.domain.IdentityContextSession;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import com.hjo2oa.org.identity.context.domain.IdentityContextView;
import com.hjo2oa.org.person.account.application.PersonAccountApplicationService.AuthenticatedAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IdentityContextAuthenticationApplicationService {

    private final IdentityContextSessionRepository sessionRepository;
    private final Clock clock;

    @Autowired
    public IdentityContextAuthenticationApplicationService(IdentityContextSessionRepository sessionRepository) {
        this(sessionRepository, Clock.systemUTC());
    }

    public IdentityContextAuthenticationApplicationService(
            IdentityContextSessionRepository sessionRepository,
            Clock clock
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public IdentityContextView establish(AuthenticatedAccount account) {
        Objects.requireNonNull(account, "account must not be null");
        IdentityContextSession template = sessionRepository.currentSession();
        IdentityContextSession session = new IdentityContextSession(
                account.tenantId().toString(),
                account.personId().toString(),
                account.accountId().toString(),
                template.currentAssignmentId(),
                template.permissionSnapshotVersion() + 1,
                Instant.now(clock),
                template.assignments()
        );
        return sessionRepository.save(session).currentContext();
    }

    public IdentityContextView establishFallback(AuthenticatedAccount account) {
        Objects.requireNonNull(account, "account must not be null");
        Instant now = Instant.now(clock);
        String assignmentId = "assignment-" + account.personId();
        IdentityContextSession session = new IdentityContextSession(
                account.tenantId().toString(),
                account.personId().toString(),
                account.accountId().toString(),
                assignmentId,
                1L,
                now,
                List.of(new IdentityAssignment(
                        assignmentId,
                        "position-" + account.personId(),
                        "org-" + account.tenantId(),
                        "dept-" + account.tenantId(),
                        "Authenticated User",
                        "Default Organization",
                        "Default Department",
                        IdentityAssignmentType.PRIMARY,
                        true,
                        List.of("role-portal-user"),
                        null
                ))
        );
        return sessionRepository.save(session).currentContext();
    }
}
