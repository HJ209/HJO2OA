package com.hjo2oa.org.data.permission.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataPermissionRepository {

    Optional<DataPermission> findRowPolicyById(UUID policyId);

    List<DataPermission> findRowPolicies(DataPermissionQuery query);

    DataPermission saveRowPolicy(DataPermission policy);

    void deleteRowPolicy(UUID policyId);

    Optional<FieldPermission> findFieldPolicyById(UUID policyId);

    List<FieldPermission> findFieldPolicies(FieldPermissionQuery query);

    FieldPermission saveFieldPolicy(FieldPermission policy);

    void deleteFieldPolicy(UUID policyId);
}
