package com.hjo2oa.org.identity.context.infrastructure;

import com.hjo2oa.org.identity.context.domain.IdentityAssignment;
import com.hjo2oa.org.identity.context.domain.IdentityAssignmentType;
import com.hjo2oa.org.identity.context.domain.IdentityContextSession;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;

@Repository
public class InMemoryIdentityContextSessionRepository implements IdentityContextSessionRepository {

    private final AtomicReference<IdentityContextSession> currentSession;
    @Autowired
    public InMemoryIdentityContextSessionRepository() {
        this(createDefaultSession());
    }
    public InMemoryIdentityContextSessionRepository(IdentityContextSession seedSession) {
        this.currentSession = new AtomicReference<>(Objects.requireNonNull(seedSession, "seedSession must not be null"));
    }

    @Override
    public IdentityContextSession currentSession() {
        return currentSession.get();
    }

    @Override
    public IdentityContextSession save(IdentityContextSession session) {
        currentSession.set(Objects.requireNonNull(session, "session must not be null"));
        return session;
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
                                "POSITION_DISABLED"
                        )
                )
        );
    }
}
