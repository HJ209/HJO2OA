package com.hjo2oa.org.person.account.infrastructure;

import com.hjo2oa.org.person.account.application.PersonAssignmentLink;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcPersonAssignmentLink implements PersonAssignmentLink {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPersonAssignmentLink(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int closeActiveAssignments(UUID tenantId, UUID personId, LocalDate endDate, Instant changedAt) {
        var rows = jdbcTemplate.query(
                """
                select id, position_id, type, start_date, end_date, status
                from org_assignment
                where tenant_id = ? and person_id = ? and status = 'ACTIVE'
                """,
                (rs, rowNum) -> new AssignmentRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("position_id", UUID.class),
                        rs.getString("type"),
                        rs.getObject("start_date", LocalDate.class),
                        rs.getObject("end_date", LocalDate.class),
                        rs.getString("status")
                ),
                tenantId,
                personId
        );
        for (AssignmentRow row : rows) {
            jdbcTemplate.update(
                    """
                    insert into org_assignment_history (
                        id, assignment_id, person_id, position_id, type, start_date, end_date,
                        status, action, tenant_id, changed_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID(),
                    row.id(),
                    personId,
                    row.positionId(),
                    row.type(),
                    row.startDate(),
                    endDate == null ? row.endDate() : endDate,
                    "INACTIVE",
                    "PERSON_STATUS_CHANGED",
                    tenantId,
                    changedAt
            );
        }
        return jdbcTemplate.update(
                """
                update org_assignment
                set status = 'INACTIVE',
                    end_date = coalesce(?, end_date),
                    updated_at = ?
                where tenant_id = ? and person_id = ? and status = 'ACTIVE'
                """,
                endDate,
                changedAt,
                tenantId,
                personId
        );
    }

    private record AssignmentRow(
            UUID id,
            UUID positionId,
            String type,
            LocalDate startDate,
            LocalDate endDate,
            String status
    ) {
    }
}
