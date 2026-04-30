package com.hjo2oa.org.identity.context.infrastructure;

import com.hjo2oa.infra.security.infrastructure.jwt.JwtAuthenticationToken;
import com.hjo2oa.org.identity.context.domain.IdentityAssignment;
import com.hjo2oa.org.identity.context.domain.IdentityAssignmentType;
import com.hjo2oa.org.identity.context.domain.IdentityContextSession;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;

@Repository
public class InMemoryIdentityContextSessionRepository implements IdentityContextSessionRepository {

    private final AtomicReference<IdentityContextSession> currentSession;
    private final Map<String, IdentityContextSession> sessionsByAccountId = new ConcurrentHashMap<>();
    @Autowired
    public InMemoryIdentityContextSessionRepository() {
        this(createDefaultSession());
    }
    public InMemoryIdentityContextSessionRepository(IdentityContextSession seedSession) {
        this.currentSession = new AtomicReference<>(Objects.requireNonNull(seedSession, "seedSession must not be null"));
    }

    @Override
    public IdentityContextSession currentSession() {
        String accountId = currentAccountId();
        if (accountId != null && sessionsByAccountId.containsKey(accountId)) {
            return sessionsByAccountId.get(accountId);
        }
        return currentSession.get();
    }

    @Override
    public IdentityContextSession save(IdentityContextSession session) {
        currentSession.set(Objects.requireNonNull(session, "session must not be null"));
        sessionsByAccountId.put(session.accountId(), session);
        return session;
    }

    private String currentAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.claims().accountId();
        }
        return null;
    }

    private static IdentityContextSession createDefaultSession() {
        return new IdentityContextSession(
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                1L,
                Instant.parse("2026-04-19T00:00:00Z"),
                List.of(
                        new IdentityAssignment(
                                "assignment-1",
                                "position-1",
                                "org-1",
                                "dept-1",
                                "Primary Position",
                                "Headquarters",
                                "General Affairs",
                                IdentityAssignmentType.PRIMARY,
                                true,
                                List.of("role-portal-user", "role-org-admin"),
                                List.of("MENU:portal.home:READ", "API:/api/v1/org/**:READ"),
                                null
                        ),
                        new IdentityAssignment(
                                "assignment-2",
                                "position-2",
                                "org-1",
                                "dept-2",
                                "Workflow Approver",
                                "Headquarters",
                                "Workflow Center",
                                IdentityAssignmentType.SECONDARY,
                                true,
                                List.of("role-workflow-approver", "role-content-reviewer"),
                                List.of("MENU:workflow.approval:READ"),
                                null
                        ),
                        new IdentityAssignment(
                                "assignment-3",
                                "position-3",
                                "org-1",
                                "dept-3",
                                "Disabled Position",
                                "Headquarters",
                                "Archive Center",
                                IdentityAssignmentType.SECONDARY,
                                false,
                                List.of("role-archive-admin"),
                                List.of("MENU:archive.admin:READ"),
                                "POSITION_DISABLED"
                        )
                )
        );
    }
}
