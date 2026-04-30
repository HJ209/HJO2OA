package com.hjo2oa.org.org.structure.infrastructure;

import com.hjo2oa.org.org.structure.application.OrgStructureReferenceValidator;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcOrgStructureReferenceValidator implements OrgStructureReferenceValidator {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOrgStructureReferenceValidator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void ensureOrganizationCanBeDeleted(UUID tenantId, UUID organizationId) {
        ensureNoRows(
                "select count(1) from org_position where tenant_id = ? and organization_id = ?",
                tenantId,
                organizationId,
                "Organization has positions"
        );
        ensureNoRows(
                "select count(1) from org_person where tenant_id = ? and organization_id = ?",
                tenantId,
                organizationId,
                "Organization has persons"
        );
    }

    @Override
    public void ensureDepartmentCanBeDeleted(UUID tenantId, UUID departmentId) {
        ensureNoRows(
                "select count(1) from org_position where tenant_id = ? and department_id = ?",
                tenantId,
                departmentId,
                "Department has positions"
        );
        ensureNoRows(
                "select count(1) from org_person where tenant_id = ? and department_id = ?",
                tenantId,
                departmentId,
                "Department has persons"
        );
    }

    private void ensureNoRows(String sql, UUID tenantId, UUID scopeId, String message) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, scopeId);
        if (count != null && count > 0) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, message);
        }
    }
}
