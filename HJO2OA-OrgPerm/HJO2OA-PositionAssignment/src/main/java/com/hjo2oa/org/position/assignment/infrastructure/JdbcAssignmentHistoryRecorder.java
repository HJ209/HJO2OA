package com.hjo2oa.org.position.assignment.infrastructure;

import com.hjo2oa.org.position.assignment.application.AssignmentHistoryRecorder;
import com.hjo2oa.org.position.assignment.domain.Assignment;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcAssignmentHistoryRecorder implements AssignmentHistoryRecorder {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAssignmentHistoryRecorder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(Assignment assignment, String action, Instant changedAt) {
        jdbcTemplate.update(
                """
                insert into org_assignment_history (
                    id, assignment_id, person_id, position_id, type, start_date, end_date,
                    status, action, tenant_id, changed_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                assignment.id(),
                assignment.personId(),
                assignment.positionId(),
                assignment.type().name(),
                assignment.startDate(),
                assignment.endDate(),
                assignment.status().name(),
                action,
                assignment.tenantId(),
                changedAt
        );
    }
}
