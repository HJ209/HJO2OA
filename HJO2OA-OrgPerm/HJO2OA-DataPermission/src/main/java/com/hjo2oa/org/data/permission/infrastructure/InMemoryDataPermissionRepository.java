package com.hjo2oa.org.data.permission.infrastructure;

import com.hjo2oa.org.data.permission.domain.DataPermission;
import com.hjo2oa.org.data.permission.domain.DataPermissionQuery;
import com.hjo2oa.org.data.permission.domain.DataPermissionRepository;
import com.hjo2oa.org.data.permission.domain.FieldPermission;
import com.hjo2oa.org.data.permission.domain.FieldPermissionQuery;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryDataPermissionRepository implements DataPermissionRepository {

    private final Map<UUID, DataPermission> rowPoliciesById = new LinkedHashMap<>();
    private final Map<UUID, FieldPermission> fieldPoliciesById = new LinkedHashMap<>();

    @Override
    public Optional<DataPermission> findRowPolicyById(UUID policyId) {
        return Optional.ofNullable(rowPoliciesById.get(policyId));
    }

    @Override
    public List<DataPermission> findRowPolicies(DataPermissionQuery query) {
        return rowPoliciesById.values().stream()
                .filter(policy -> rowMatches(policy, query))
                .toList();
    }

    @Override
    public DataPermission saveRowPolicy(DataPermission policy) {
        rowPoliciesById.put(policy.id(), Objects.requireNonNull(policy, "policy must not be null"));
        return policy;
    }

    @Override
    public void deleteRowPolicy(UUID policyId) {
        rowPoliciesById.remove(policyId);
    }

    @Override
    public Optional<FieldPermission> findFieldPolicyById(UUID policyId) {
        return Optional.ofNullable(fieldPoliciesById.get(policyId));
    }

    @Override
    public List<FieldPermission> findFieldPolicies(FieldPermissionQuery query) {
        return fieldPoliciesById.values().stream()
                .filter(policy -> fieldMatches(policy, query))
                .toList();
    }

    @Override
    public FieldPermission saveFieldPolicy(FieldPermission policy) {
        fieldPoliciesById.put(policy.id(), Objects.requireNonNull(policy, "policy must not be null"));
        return policy;
    }

    @Override
    public void deleteFieldPolicy(UUID policyId) {
        fieldPoliciesById.remove(policyId);
    }

    private boolean rowMatches(DataPermission policy, DataPermissionQuery query) {
        if (query == null) {
            return true;
        }
        return matches(query.subjectType(), policy.subjectType())
                && matches(query.subjectId(), policy.subjectId())
                && matches(query.businessObject(), policy.businessObject())
                && matches(query.scopeType(), policy.scopeType())
                && matches(query.effect(), policy.effect())
                && matches(query.tenantId(), policy.tenantId());
    }

    private boolean fieldMatches(FieldPermission policy, FieldPermissionQuery query) {
        if (query == null) {
            return true;
        }
        return matches(query.subjectType(), policy.subjectType())
                && matches(query.subjectId(), policy.subjectId())
                && matches(query.businessObject(), policy.businessObject())
                && matches(query.usageScenario(), policy.usageScenario())
                && matches(query.fieldCode(), policy.fieldCode())
                && matches(query.action(), policy.action())
                && matches(query.effect(), policy.effect())
                && matches(query.tenantId(), policy.tenantId());
    }

    private boolean matches(Object expected, Object actual) {
        return expected == null || Objects.equals(expected, actual);
    }
}
