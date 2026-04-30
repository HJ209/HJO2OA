package com.hjo2oa.content.category.management.infrastructure;

import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.CategoryRecord;
import com.hjo2oa.content.category.management.application.ContentCategoryApplicationService.PermissionRuleRecord;
import com.hjo2oa.content.category.management.application.ContentCategoryRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryContentCategoryRepository implements ContentCategoryRepository {

    private final Map<UUID, CategoryRecord> categories = new ConcurrentHashMap<>();
    private final Map<UUID, List<PermissionRuleRecord>> permissions = new ConcurrentHashMap<>();

    @Override
    public Optional<CategoryRecord> findById(UUID tenantId, UUID id) {
        return Optional.ofNullable(categories.get(id))
                .filter(category -> category.tenantId().equals(tenantId));
    }

    @Override
    public Optional<CategoryRecord> findByCode(UUID tenantId, String code) {
        return categories.values()
                .stream()
                .filter(category -> category.tenantId().equals(tenantId))
                .filter(category -> category.code().equals(code))
                .findFirst();
    }

    @Override
    public List<CategoryRecord> findByTenantId(UUID tenantId) {
        return categories.values()
                .stream()
                .filter(category -> category.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(CategoryRecord::sortOrder).thenComparing(CategoryRecord::name))
                .toList();
    }

    @Override
    public void save(CategoryRecord category) {
        categories.put(category.id(), category);
    }

    @Override
    public void replacePermissions(UUID tenantId, UUID categoryId, List<PermissionRuleRecord> rules) {
        permissions.put(categoryId, rules == null ? List.of() : List.copyOf(rules));
    }

    @Override
    public List<PermissionRuleRecord> findPermissions(UUID tenantId, UUID categoryId) {
        return new ArrayList<>(permissions.getOrDefault(categoryId, List.of()));
    }
}
