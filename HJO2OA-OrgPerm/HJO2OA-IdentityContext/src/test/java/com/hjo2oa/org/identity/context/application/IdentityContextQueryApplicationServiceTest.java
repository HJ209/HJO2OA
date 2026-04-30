package com.hjo2oa.org.identity.context.application;

import com.hjo2oa.org.identity.context.domain.AvailableIdentityOption;
import com.hjo2oa.org.identity.context.domain.IdentityAssignment;
import com.hjo2oa.org.identity.context.domain.IdentityAssignmentType;
import com.hjo2oa.org.identity.context.domain.IdentityContextSession;
import com.hjo2oa.org.identity.context.domain.IdentityContextView;
import com.hjo2oa.org.identity.context.infrastructure.InMemoryIdentityContextSessionRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityContextQueryApplicationServiceTest {

    @Test
    void shouldReturnCurrentContextAndAvailableAssignments() {
        IdentityContextQueryApplicationService service = new IdentityContextQueryApplicationService(
                new InMemoryIdentityContextSessionRepository()
        );

        IdentityContextView currentContext = service.current();
        List<AvailableIdentityOption> availableOptions = service.available(true);
        List<AvailableIdentityOption> secondaryOnly = service.available(false);

        assertEquals("assignment-1", currentContext.currentAssignmentId());
        assertEquals("position-1", currentContext.currentPositionId());
        assertEquals(3, availableOptions.size());
        assertEquals(2, secondaryOnly.size());
        assertTrue(availableOptions.stream().anyMatch(AvailableIdentityOption::current));
        assertTrue(availableOptions.stream().anyMatch(AvailableIdentityOption::switchable));
        assertFalse(secondaryOnly.stream().anyMatch(option -> option.assignmentType().name().equals("PRIMARY")));
    }

    @Test
    void shouldReturnAvailableAssignmentWithoutDepartment() {
        IdentityContextSession session = new IdentityContextSession(
                "tenant-1",
                "person-1",
                "account-1",
                "assignment-1",
                1L,
                Instant.parse("2026-04-19T00:00:00Z"),
                List.of(new IdentityAssignment(
                        "assignment-1",
                        "position-1",
                        "org-1",
                        null,
                        "Primary Position",
                        "Headquarters",
                        null,
                        IdentityAssignmentType.PRIMARY,
                        true,
                        List.of("role-portal-user"),
                        List.of("API:/api/**:READ"),
                        null
                ))
        );
        IdentityContextQueryApplicationService service = new IdentityContextQueryApplicationService(
                new InMemoryIdentityContextSessionRepository(session)
        );

        List<AvailableIdentityOption> availableOptions = service.available(true);

        assertEquals(1, availableOptions.size());
        assertNull(availableOptions.get(0).departmentId());
        assertNull(availableOptions.get(0).departmentName());
    }
}
