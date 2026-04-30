package com.hjo2oa.org.position.assignment.infrastructure;

import com.hjo2oa.org.position.assignment.application.PositionAssignmentReferenceValidator;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcPositionAssignmentReferenceValidator implements PositionAssignmentReferenceValidator {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPositionAssignmentReferenceValidator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void ensurePositionScopeActive(UUID tenantId, UUID organizationId, UUID departmentId) {
        Integer organizationCount = jdbcTemplate.queryForObject(
                "select count(1) from org_organization where tenant_id = ? and id = ? and status = 'ACTIVE'",
                Integer.class,
                tenantId,
                organizationId
        );
        if (organizationCount == null || organizationCount == 0) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Organization is missing or disabled");
        }
        if (departmentId == null) {
            return;
        }
        Integer departmentCount = jdbcTemplate.queryForObject(
                "select count(1) from org_department where tenant_id = ? and id = ? and organization_id = ? and status = 'ACTIVE'",
                Integer.class,
                tenantId,
                departmentId,
                organizationId
        );
        if (departmentCount == null || departmentCount == 0) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Department is missing or disabled");
        }
    }

    @Override
    public void ensurePersonAssignable(UUID tenantId, UUID personId) {
        Integer personCount = jdbcTemplate.queryForObject(
                "select count(1) from org_person where tenant_id = ? and id = ? and status = 'ACTIVE'",
                Integer.class,
                tenantId,
                personId
        );
        if (personCount == null || personCount == 0) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Person is missing or not active");
        }
    }
}
