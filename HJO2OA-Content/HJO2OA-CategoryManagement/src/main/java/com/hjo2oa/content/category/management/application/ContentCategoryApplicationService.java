package com.hjo2oa.content.category.management.application;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEvent;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentCategoryApplicationService {

    private final ContentCategoryRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public ContentCategoryApplicationService(ContentCategoryRepository repository) {
        this(repository, event -> {
        }, Clock.systemUTC());
    }

    public ContentCategoryApplicationService(
            ContentCategoryRepository repository,
            DomainEventPublisher eventPublisher,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public CategoryView create(CreateCategoryCommand command) {
        validateCommandTenant(command.tenantId());
        String code = requireText(command.code(), "code");
        Optional<CategoryRecord> duplicate = repository.findByCode(command.tenantId(), code);
        if (duplicate.isPresent() && sameCategory(duplicate.get(), command)) {
            return toView(duplicate.get());
        }
        if (duplicate.isPresent()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Category code already exists");
        }
        if (command.parentId() != null) {
            requireCategory(command.tenantId(), command.parentId());
        }
        Instant now = clock.instant();
        CategoryRecord category = new CategoryRecord(
                UUID.randomUUID(),
                code,
                requireText(command.name(), "name"),
                defaultText(command.categoryType(), "GENERAL"),
                command.parentId(),
                normalizePath(command.routePath()),
                command.sortOrder(),
                defaultText(command.visibleMode(), "INHERIT"),
                CategoryStatus.ENABLED,
                1,
                command.tenantId(),
                command.operatorId(),
                command.operatorId(),
                now,
                now
        );
        repository.save(category);
        repository.replacePermissions(
                category.tenantId(),
                category.id(),
                toRuleRecords(category.tenantId(), category.id(), command.permissions())
        );
        publish(new ContentCategoryChangedEvent(
                UUID.randomUUID(),
                "content.category.created",
                now,
                category.tenantId().toString(),
                category.id(),
                category.parentId(),
                command.operatorId()
        ));
        return toView(category);
    }

    @Transactional
    public CategoryView update(UUID categoryId, UpdateCategoryCommand command) {
        validateCommandTenant(command.tenantId());
        CategoryRecord existing = requireCategory(command.tenantId(), categoryId);
        if (sameCategory(existing, command)) {
            return toView(existing);
        }
        if (!existing.code().equals(command.code()) && repository.findByCode(command.tenantId(), command.code()).isPresent()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Category code already exists");
        }
        if (command.parentId() != null) {
            CategoryRecord parent = requireCategory(command.tenantId(), command.parentId());
            assertNotMovingIntoChild(existing, parent, command.tenantId());
        }
        Instant now = clock.instant();
        CategoryRecord updated = new CategoryRecord(
                existing.id(),
                requireText(command.code(), "code"),
                requireText(command.name(), "name"),
                defaultText(command.categoryType(), existing.categoryType()),
                command.parentId(),
                normalizePath(command.routePath()),
                command.sortOrder(),
                defaultText(command.visibleMode(), existing.visibleMode()),
                existing.status(),
                existing.versionNo() + 1,
                existing.tenantId(),
                existing.createdBy(),
                command.operatorId(),
                existing.createdAt(),
                now
        );
        repository.save(updated);
        if (command.permissions() != null) {
            repository.replacePermissions(
                    updated.tenantId(),
                    updated.id(),
                    toRuleRecords(updated.tenantId(), updated.id(), command.permissions())
            );
        }
        publish(new ContentCategoryChangedEvent(
                UUID.randomUUID(),
                "content.category.updated",
                now,
                updated.tenantId().toString(),
                updated.id(),
                updated.parentId(),
                command.operatorId()
        ));
        return toView(updated);
    }

    @Transactional
    public CategoryView move(UUID categoryId, MoveCategoryCommand command) {
        validateCommandTenant(command.tenantId());
        CategoryRecord existing = requireCategory(command.tenantId(), categoryId);
        if (command.expectedVersionNo() != null && command.expectedVersionNo() != existing.versionNo()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Category version conflict");
        }
        if (Objects.equals(existing.parentId(), command.parentId()) && existing.sortOrder() == command.sortOrder()) {
            return toView(existing);
        }
        CategoryRecord parent = null;
        if (command.parentId() != null) {
            parent = requireCategory(command.tenantId(), command.parentId());
            assertNotMovingIntoChild(existing, parent, command.tenantId());
        }
        Instant now = clock.instant();
        CategoryRecord moved = new CategoryRecord(
                existing.id(),
                existing.code(),
                existing.name(),
                existing.categoryType(),
                parent == null ? null : parent.id(),
                existing.routePath(),
                command.sortOrder(),
                existing.visibleMode(),
                existing.status(),
                existing.versionNo() + 1,
                existing.tenantId(),
                existing.createdBy(),
                command.operatorId(),
                existing.createdAt(),
                now
        );
        repository.save(moved);
        publish(new ContentCategoryChangedEvent(
                UUID.randomUUID(),
                "content.category.moved",
                now,
                moved.tenantId().toString(),
                moved.id(),
                moved.parentId(),
                command.operatorId()
        ));
        return toView(moved);
    }

    @Transactional
    public CategoryView enable(UUID tenantId, UUID categoryId, UUID operatorId) {
        return changeStatus(tenantId, categoryId, operatorId, CategoryStatus.ENABLED, "content.category.enabled");
    }

    @Transactional
    public CategoryView disable(UUID tenantId, UUID categoryId, UUID operatorId) {
        return changeStatus(tenantId, categoryId, operatorId, CategoryStatus.DISABLED, "content.category.disabled");
    }

    @Transactional
    public List<PermissionRuleView> replacePermissions(
            UUID tenantId,
            UUID categoryId,
            UUID operatorId,
            List<PermissionRuleInput> inputs
    ) {
        validateCommandTenant(tenantId);
        CategoryRecord category = requireCategory(tenantId, categoryId);
        List<PermissionRuleRecord> rules = toRuleRecords(tenantId, categoryId, inputs);
        if (sameRules(repository.findPermissions(tenantId, categoryId), rules)) {
            return repository.findPermissions(tenantId, categoryId).stream()
                    .map(ContentCategoryApplicationService::toPermissionView)
                    .toList();
        }
        repository.replacePermissions(tenantId, categoryId, rules);
        publish(new ContentCategoryChangedEvent(
                UUID.randomUUID(),
                "content.category.permission.updated",
                clock.instant(),
                tenantId.toString(),
                category.id(),
                category.parentId(),
                operatorId
        ));
        return rules.stream().map(ContentCategoryApplicationService::toPermissionView).toList();
    }

    @Transactional(readOnly = true)
    public CategoryView get(UUID tenantId, UUID categoryId) {
        validateCommandTenant(tenantId);
        return toView(requireCategory(tenantId, categoryId));
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeNode> tree(UUID tenantId, Boolean enabledOnly) {
        validateCommandTenant(tenantId);
        List<CategoryRecord> categories = repository.findByTenantId(tenantId)
                .stream()
                .filter(category -> !Boolean.TRUE.equals(enabledOnly) || category.status() == CategoryStatus.ENABLED)
                .sorted(Comparator.comparing(CategoryRecord::sortOrder).thenComparing(CategoryRecord::name))
                .toList();
        return buildTree(categories, null);
    }

    @Transactional(readOnly = true)
    public List<PermissionRuleView> permissions(UUID tenantId, UUID categoryId) {
        validateCommandTenant(tenantId);
        requireCategory(tenantId, categoryId);
        return repository.findPermissions(tenantId, categoryId).stream()
                .map(ContentCategoryApplicationService::toPermissionView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissionPreviewResult previewPermission(
            UUID tenantId,
            UUID categoryId,
            PermissionSubjectContext subject,
            PermissionScope scope
    ) {
        validateCommandTenant(tenantId);
        requireCategory(tenantId, categoryId);
        List<PermissionRuleRecord> matched = matchingRules(tenantId, categoryId, subject, scope);
        boolean denied = matched.stream().anyMatch(rule -> rule.effect() == PermissionEffect.DENY);
        boolean allowed = !denied && (matched.isEmpty()
                || matched.stream().anyMatch(rule -> rule.effect() == PermissionEffect.ALLOW));
        return new PermissionPreviewResult(
                allowed,
                denied ? "DENY_RULE_MATCHED" : "ALLOW",
                matched.stream().map(ContentCategoryApplicationService::toPermissionView).toList()
        );
    }

    @Transactional(readOnly = true)
    public boolean canManage(UUID tenantId, UUID categoryId, UUID operatorId, Set<UUID> roleIds) {
        return previewPermission(
                tenantId,
                categoryId,
                new PermissionSubjectContext(operatorId, null, null, null, roleIds),
                PermissionScope.MANAGE
        ).allowed();
    }

    @Transactional(readOnly = true)
    public Optional<CategoryRecord> findRecord(UUID tenantId, UUID categoryId) {
        if (tenantId == null || categoryId == null) {
            return Optional.empty();
        }
        return repository.findById(tenantId, categoryId);
    }

    private CategoryView changeStatus(
            UUID tenantId,
            UUID categoryId,
            UUID operatorId,
            CategoryStatus status,
            String eventType
    ) {
        validateCommandTenant(tenantId);
        CategoryRecord existing = requireCategory(tenantId, categoryId);
        if (existing.status() == status) {
            return toView(existing);
        }
        Instant now = clock.instant();
        CategoryRecord updated = new CategoryRecord(
                existing.id(),
                existing.code(),
                existing.name(),
                existing.categoryType(),
                existing.parentId(),
                existing.routePath(),
                existing.sortOrder(),
                existing.visibleMode(),
                status,
                existing.versionNo() + 1,
                existing.tenantId(),
                existing.createdBy(),
                operatorId,
                existing.createdAt(),
                now
        );
        repository.save(updated);
        publish(new ContentCategoryChangedEvent(
                UUID.randomUUID(),
                eventType,
                now,
                tenantId.toString(),
                updated.id(),
                updated.parentId(),
                operatorId
        ));
        return toView(updated);
    }

    private CategoryRecord requireCategory(UUID tenantId, UUID categoryId) {
        return repository.findById(tenantId, categoryId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Category not found"));
    }

    private void assertNotMovingIntoChild(CategoryRecord source, CategoryRecord targetParent, UUID tenantId) {
        UUID cursor = targetParent.id();
        Set<UUID> seen = new HashSet<>();
        while (cursor != null && seen.add(cursor)) {
            if (cursor.equals(source.id())) {
                throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Category cannot move into itself or children");
            }
            cursor = repository.findById(tenantId, cursor).map(CategoryRecord::parentId).orElse(null);
        }
    }

    private List<PermissionRuleRecord> matchingRules(
            UUID tenantId,
            UUID categoryId,
            PermissionSubjectContext subject,
            PermissionScope scope
    ) {
        return repository.findPermissions(tenantId, categoryId)
                .stream()
                .filter(rule -> rule.scope() == scope || rule.scope() == PermissionScope.ALL)
                .filter(rule -> matchesSubject(rule, subject))
                .sorted(Comparator.comparing(PermissionRuleRecord::sortOrder))
                .toList();
    }

    private static boolean matchesSubject(PermissionRuleRecord rule, PermissionSubjectContext subject) {
        if (rule.subjectType() == PermissionSubjectType.ALL) {
            return true;
        }
        if (subject == null) {
            return false;
        }
        UUID subjectId = rule.subjectId();
        return switch (rule.subjectType()) {
            case PERSON -> subjectId != null && subjectId.equals(subject.personId());
            case ASSIGNMENT -> subjectId != null && subjectId.equals(subject.assignmentId());
            case POSITION -> subjectId != null && subjectId.equals(subject.positionId());
            case DEPARTMENT -> subjectId != null && subjectId.equals(subject.departmentId());
            case ROLE -> subjectId != null && subject.roleIds().contains(subjectId);
            case ALL -> true;
        };
    }

    private List<CategoryTreeNode> buildTree(List<CategoryRecord> categories, UUID parentId) {
        List<CategoryTreeNode> nodes = new ArrayList<>();
        for (CategoryRecord category : categories) {
            if (Objects.equals(category.parentId(), parentId)) {
                nodes.add(new CategoryTreeNode(toView(category), buildTree(categories, category.id())));
            }
        }
        return nodes;
    }

    private CategoryView toView(CategoryRecord category) {
        List<PermissionRuleView> rules = repository.findPermissions(category.tenantId(), category.id())
                .stream()
                .map(ContentCategoryApplicationService::toPermissionView)
                .toList();
        return new CategoryView(
                category.id(),
                category.code(),
                category.name(),
                category.categoryType(),
                category.parentId(),
                category.routePath(),
                category.sortOrder(),
                category.visibleMode(),
                category.status(),
                category.versionNo(),
                category.tenantId(),
                category.createdBy(),
                category.updatedBy(),
                category.createdAt(),
                category.updatedAt(),
                rules
        );
    }

    private static PermissionRuleView toPermissionView(PermissionRuleRecord rule) {
        return new PermissionRuleView(
                rule.id(),
                rule.subjectType(),
                rule.subjectId(),
                rule.effect(),
                rule.scope(),
                rule.sortOrder()
        );
    }

    private static List<PermissionRuleRecord> toRuleRecords(
            UUID tenantId,
            UUID categoryId,
            List<PermissionRuleInput> inputs
    ) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        List<PermissionRuleRecord> records = new ArrayList<>();
        int index = 0;
        for (PermissionRuleInput input : inputs) {
            PermissionSubjectType subjectType = input.subjectType() == null
                    ? PermissionSubjectType.ALL
                    : input.subjectType();
            UUID subjectId = subjectType == PermissionSubjectType.ALL ? null : input.subjectId();
            records.add(new PermissionRuleRecord(
                    UUID.randomUUID(),
                    categoryId,
                    tenantId,
                    subjectType,
                    subjectId,
                    input.effect() == null ? PermissionEffect.ALLOW : input.effect(),
                    input.scope() == null ? PermissionScope.READ : input.scope(),
                    input.sortOrder() == null ? index : input.sortOrder(),
                    Instant.now()
            ));
            index++;
        }
        return records;
    }

    private boolean sameCategory(CategoryRecord existing, CreateCategoryCommand command) {
        return Objects.equals(existing.code(), requireText(command.code(), "code"))
                && Objects.equals(existing.name(), requireText(command.name(), "name"))
                && Objects.equals(existing.categoryType(), defaultText(command.categoryType(), "GENERAL"))
                && Objects.equals(existing.parentId(), command.parentId())
                && Objects.equals(existing.routePath(), normalizePath(command.routePath()))
                && existing.sortOrder() == command.sortOrder()
                && Objects.equals(existing.visibleMode(), defaultText(command.visibleMode(), "INHERIT"))
                && sameRules(
                        repository.findPermissions(existing.tenantId(), existing.id()),
                        toRuleRecords(existing.tenantId(), existing.id(), command.permissions())
                );
    }

    private boolean sameCategory(CategoryRecord existing, UpdateCategoryCommand command) {
        return Objects.equals(existing.code(), requireText(command.code(), "code"))
                && Objects.equals(existing.name(), requireText(command.name(), "name"))
                && Objects.equals(existing.categoryType(), defaultText(command.categoryType(), existing.categoryType()))
                && Objects.equals(existing.parentId(), command.parentId())
                && Objects.equals(existing.routePath(), normalizePath(command.routePath()))
                && existing.sortOrder() == command.sortOrder()
                && Objects.equals(existing.visibleMode(), defaultText(command.visibleMode(), existing.visibleMode()))
                && (command.permissions() == null || sameRules(
                        repository.findPermissions(existing.tenantId(), existing.id()),
                        toRuleRecords(existing.tenantId(), existing.id(), command.permissions())
                ));
    }

    private static boolean sameRules(List<PermissionRuleRecord> current, List<PermissionRuleRecord> requested) {
        List<PermissionRuleRecord> left = current == null ? List.of() : current;
        List<PermissionRuleRecord> right = requested == null ? List.of() : requested;
        if (left.size() != right.size()) {
            return false;
        }
        for (int index = 0; index < left.size(); index++) {
            PermissionRuleRecord currentRule = left.get(index);
            PermissionRuleRecord requestedRule = right.get(index);
            if (currentRule.subjectType() != requestedRule.subjectType()
                    || !Objects.equals(currentRule.subjectId(), requestedRule.subjectId())
                    || currentRule.effect() != requestedRule.effect()
                    || currentRule.scope() != requestedRule.scope()
                    || currentRule.sortOrder() != requestedRule.sortOrder()) {
                return false;
            }
        }
        return true;
    }

    private static void validateCommandTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new BizException(SharedErrorDescriptors.TENANT_REQUIRED, "Tenant id is required");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BizException(SharedErrorDescriptors.VALIDATION_ERROR, fieldName + " is required");
        }
        return value.trim();
    }

    private static String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private void publish(DomainEvent event) {
        eventPublisher.publish(event);
    }

    public enum CategoryStatus {
        ENABLED,
        DISABLED
    }

    public enum PermissionSubjectType {
        ALL,
        PERSON,
        ASSIGNMENT,
        POSITION,
        DEPARTMENT,
        ROLE
    }

    public enum PermissionEffect {
        ALLOW,
        DENY
    }

    public enum PermissionScope {
        ALL,
        READ,
        MANAGE
    }

    public record CategoryRecord(
            UUID id,
            String code,
            String name,
            String categoryType,
            UUID parentId,
            String routePath,
            int sortOrder,
            String visibleMode,
            CategoryStatus status,
            int versionNo,
            UUID tenantId,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PermissionRuleRecord(
            UUID id,
            UUID categoryId,
            UUID tenantId,
            PermissionSubjectType subjectType,
            UUID subjectId,
            PermissionEffect effect,
            PermissionScope scope,
            int sortOrder,
            Instant createdAt
    ) {
    }

    public record PermissionRuleInput(
            PermissionSubjectType subjectType,
            UUID subjectId,
            PermissionEffect effect,
            PermissionScope scope,
            Integer sortOrder
    ) {
    }

    public record PermissionSubjectContext(
            UUID personId,
            UUID assignmentId,
            UUID positionId,
            UUID departmentId,
            Set<UUID> roleIds
    ) {

        public PermissionSubjectContext {
            roleIds = roleIds == null ? Set.of() : Set.copyOf(roleIds);
        }
    }

    public record CreateCategoryCommand(
            UUID tenantId,
            UUID operatorId,
            String code,
            String name,
            String categoryType,
            UUID parentId,
            String routePath,
            int sortOrder,
            String visibleMode,
            List<PermissionRuleInput> permissions,
            String idempotencyKey
    ) {
    }

    public record UpdateCategoryCommand(
            UUID tenantId,
            UUID operatorId,
            String code,
            String name,
            String categoryType,
            UUID parentId,
            String routePath,
            int sortOrder,
            String visibleMode,
            List<PermissionRuleInput> permissions,
            String idempotencyKey
    ) {
    }

    public record MoveCategoryCommand(
            UUID tenantId,
            UUID operatorId,
            UUID parentId,
            int sortOrder,
            Integer expectedVersionNo,
            String idempotencyKey
    ) {
    }

    public record CategoryView(
            UUID id,
            String code,
            String name,
            String categoryType,
            UUID parentId,
            String routePath,
            int sortOrder,
            String visibleMode,
            CategoryStatus status,
            int versionNo,
            UUID tenantId,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt,
            List<PermissionRuleView> permissions
    ) {

        public CategoryView {
            permissions = permissions == null ? List.of() : List.copyOf(permissions);
        }
    }

    public record CategoryTreeNode(CategoryView category, List<CategoryTreeNode> children) {

        public CategoryTreeNode {
            children = children == null ? List.of() : List.copyOf(children);
        }

        public List<CategoryTreeNode> flattened() {
            ArrayDeque<CategoryTreeNode> queue = new ArrayDeque<>();
            List<CategoryTreeNode> result = new ArrayList<>();
            queue.add(this);
            while (!queue.isEmpty()) {
                CategoryTreeNode current = queue.removeFirst();
                result.add(current);
                queue.addAll(current.children());
            }
            return result;
        }
    }

    public record PermissionRuleView(
            UUID id,
            PermissionSubjectType subjectType,
            UUID subjectId,
            PermissionEffect effect,
            PermissionScope scope,
            int sortOrder
    ) {
    }

    public record PermissionPreviewResult(
            boolean allowed,
            String decision,
            List<PermissionRuleView> matchedRules
    ) {

        public PermissionPreviewResult {
            matchedRules = matchedRules == null ? List.of() : List.copyOf(matchedRules);
        }
    }

    public record ContentCategoryChangedEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            UUID categoryId,
            UUID parentId,
            UUID operatorId
    ) implements DomainEvent {
    }
}
