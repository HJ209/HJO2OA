package com.hjo2oa.content.category.management.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentCategoryRepository {

    Optional<ContentCategoryApplicationService.CategoryRecord> findById(UUID tenantId, UUID id);

    Optional<ContentCategoryApplicationService.CategoryRecord> findByCode(UUID tenantId, String code);

    List<ContentCategoryApplicationService.CategoryRecord> findByTenantId(UUID tenantId);

    void save(ContentCategoryApplicationService.CategoryRecord category);

    void replacePermissions(
            UUID tenantId,
            UUID categoryId,
            List<ContentCategoryApplicationService.PermissionRuleRecord> rules
    );

    List<ContentCategoryApplicationService.PermissionRuleRecord> findPermissions(UUID tenantId, UUID categoryId);
}
