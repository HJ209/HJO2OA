package com.hjo2oa.org.identity.context.application;

import com.hjo2oa.org.identity.context.domain.IdentityContextSession;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import com.hjo2oa.org.identity.context.domain.IdentityContextView;
import com.hjo2oa.org.person.account.application.PersonAccountApplicationService.AuthenticatedAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IdentityContextAuthenticationApplicationService {

    private final IdentityContextSessionRepository sessionRepository;
    private final IdentityContextBuilder identityContextBuilder;
    private final Clock clock;

    @Autowired
    public IdentityContextAuthenticationApplicationService(
            IdentityContextSessionRepository sessionRepository,
            IdentityContextBuilder identityContextBuilder
    ) {
        this(sessionRepository, identityContextBuilder, Clock.systemUTC());
    }

    public IdentityContextAuthenticationApplicationService(
            IdentityContextSessionRepository sessionRepository,
            Clock clock
    ) {
        this(sessionRepository, null, clock);
    }

    public IdentityContextAuthenticationApplicationService(
            IdentityContextSessionRepository sessionRepository,
            IdentityContextBuilder identityContextBuilder,
            Clock clock
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.identityContextBuilder = identityContextBuilder;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public IdentityContextView establish(AuthenticatedAccount account) {
        Objects.requireNonNull(account, "account must not be null");
        if (identityContextBuilder != null) {
            return sessionRepository.save(identityContextBuilder.build(account, null)).currentContext();
        }
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

}
