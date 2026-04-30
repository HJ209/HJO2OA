package com.hjo2oa.org.org.structure.application;

import com.hjo2oa.org.org.structure.domain.Department;
import com.hjo2oa.org.org.structure.domain.DepartmentRepository;
import com.hjo2oa.org.org.structure.domain.DepartmentView;
import com.hjo2oa.org.org.structure.domain.DeptStatus;
import com.hjo2oa.org.org.structure.domain.OrgStatus;
import com.hjo2oa.org.org.structure.domain.OrgStructureChangedEvent;
import com.hjo2oa.org.org.structure.domain.Organization;
import com.hjo2oa.org.org.structure.domain.OrganizationRepository;
import com.hjo2oa.org.org.structure.domain.OrganizationView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import com.hjo2oa.shared.tenant.TenantRequestContext;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrgStructureApplicationService {

    private static final Comparator<Organization> ORG_TREE_ORDER = Comparator
            .comparingInt(Organization::level)
            .thenComparingInt(Organization::sortOrder)
            .thenComparing(Organization::code);

    private static final Comparator<Department> DEPT_TREE_ORDER = Comparator
            .comparingInt(Department::level)
            .thenComparingInt(Department::sortOrder)
            .thenComparing(Department::code);

    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final Clock clock;
    private final DomainEventPublisher domainEventPublisher;
    private final OrgStructureReferenceValidator referenceValidator;

    @Autowired
    public OrgStructureApplicationService(
            OrganizationRepository organizationRepository,
            DepartmentRepository departmentRepository,
            ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
            ObjectProvider<OrgStructureReferenceValidator> referenceValidatorProvider
    ) {
        this(
                organizationRepository,
                departmentRepository,
                Clock.systemUTC(),
                domainEventPublisherProvider.getIfAvailable(() -> event -> { }),
                referenceValidatorProvider.getIfAvailable(() -> OrgStructureReferenceValidator.NOOP)
        );
    }

    public OrgStructureApplicationService(
            OrganizationRepository organizationRepository,
            DepartmentRepository departmentRepository,
            Clock clock
    ) {
        this(organizationRepository, departmentRepository, clock, event -> { }, OrgStructureReferenceValidator.NOOP);
    }

    public OrgStructureApplicationService(
            OrganizationRepository organizationRepository,
            DepartmentRepository departmentRepository,
            Clock clock,
            DomainEventPublisher domainEventPublisher,
            OrgStructureReferenceValidator referenceValidator
    ) {
        this.organizationRepository = Objects.requireNonNull(
                organizationRepository,
                "organizationRepository must not be null"
        );
        this.departmentRepository = Objects.requireNonNull(
                departmentRepository,
                "departmentRepository must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.referenceValidator = Objects.requireNonNull(referenceValidator, "referenceValidator must not be null");
    }

    @Transactional
    public OrganizationView createOrganization(OrgStructureCommands.CreateOrganizationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensureOrganizationCodeAvailable(command.tenantId(), command.code(), null);
        Organization parent = loadParentOrganization(command.parentId(), command.tenantId());
        ensureActiveParent(parent, "Parent organization is disabled");
        Instant now = now();
        UUID organizationId = UUID.randomUUID();
        int level = parent == null ? 0 : parent.level() + 1;
        String path = appendPath(parent == null ? "/" : parent.path(), organizationId);
        Organization organization = Organization.create(
                organizationId,
                command.code(),
                command.name(),
                command.shortName(),
                command.type(),
                command.parentId(),
                level,
                path,
                command.sortOrder(),
                command.tenantId(),
                now
        );
        Organization saved = organizationRepository.save(organization);
        publishOrganizationEvent("org.organization.created", saved, payloadOf("parentId", saved.parentId()));
        return saved.toView();
    }

    @Transactional
    public OrganizationView updateOrganization(OrgStructureCommands.UpdateOrganizationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Organization organization = loadOrganization(command.tenantId(), command.organizationId());
        ensureOrganizationCodeAvailable(organization.tenantId(), command.code(), organization.id());
        Organization saved = organizationRepository.save(organization.update(
                command.code(),
                command.name(),
                command.shortName(),
                command.type(),
                command.sortOrder(),
                now()
        ));
        publishOrganizationEvent("org.organization.updated", saved, payloadOf("parentId", saved.parentId()));
        return saved.toView();
    }

    @Transactional
    public OrganizationView moveOrganization(OrgStructureCommands.MoveOrganizationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Organization organization = loadOrganization(command.tenantId(), command.organizationId());
        if (Objects.equals(organization.parentId(), command.parentId())) {
            if (command.sortOrder() != null && command.sortOrder() != organization.sortOrder()) {
                Organization saved = organizationRepository.save(organization.update(
                        organization.code(),
                        organization.name(),
                        organization.shortName(),
                        organization.type(),
                        command.sortOrder(),
                        now()
                ));
                publishOrganizationEvent(
                        "org.organization.sorted",
                        saved,
                        Map.of("sortOrder", saved.sortOrder())
                );
                return saved.toView();
            }
            return organization.toView();
        }
        Organization parent = loadParentOrganization(command.parentId(), organization.tenantId());
        ensureActiveParent(parent, "Parent organization is disabled");
        if (parent != null && parent.path().startsWith(organization.path())) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Organization move creates cycle");
        }
        Instant now = now();
        String oldPath = organization.path();
        int oldLevel = organization.level();
        int newLevel = parent == null ? 0 : parent.level() + 1;
        String newPath = appendPath(parent == null ? "/" : parent.path(), organization.id());
        int sortOrder = command.sortOrder() == null ? organization.sortOrder() : command.sortOrder();
        Organization moved = organization.move(command.parentId(), newLevel, newPath, sortOrder, now);
        List<Organization> descendants = organizationRepository.findByPathPrefix(organization.tenantId(), oldPath)
                .stream()
                .filter(candidate -> !candidate.id().equals(organization.id()))
                .toList();
        List<Organization> changed = new ArrayList<>(descendants.size() + 1);
        changed.add(moved);
        int levelDelta = newLevel - oldLevel;
        for (Organization descendant : descendants) {
            changed.add(descendant.move(
                    descendant.parentId(),
                    descendant.level() + levelDelta,
                    descendant.path().replaceFirst(java.util.regex.Pattern.quote(oldPath), newPath),
                    descendant.sortOrder(),
                    now
            ));
        }
        organizationRepository.saveAll(changed);
        Organization saved = loadOrganization(organization.tenantId(), organization.id());
        publishOrganizationEvent(
                "org.organization.moved",
                saved,
                payloadOf("oldPath", oldPath, "newPath", newPath, "parentId", saved.parentId())
        );
        return saved.toView();
    }

    @Transactional
    public OrganizationView activateOrganization(UUID tenantId, UUID organizationId) {
        Organization saved = organizationRepository.save(loadOrganization(tenantId, organizationId).activate(now()));
        publishOrganizationEvent("org.organization.enabled", saved, Map.of());
        return saved.toView();
    }

    @Transactional
    public OrganizationView disableOrganization(UUID tenantId, UUID organizationId) {
        Organization saved = organizationRepository.save(loadOrganization(tenantId, organizationId).disable(now()));
        publishOrganizationEvent("org.organization.disabled", saved, Map.of());
        return saved.toView();
    }

    @Transactional
    public void deleteOrganization(UUID tenantId, UUID organizationId) {
        Organization organization = loadOrganization(tenantId, organizationId);
        if (!organizationRepository.findByParentId(organization.tenantId(), organization.id()).isEmpty()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Organization has child organizations");
        }
        if (!departmentRepository.findByOrganizationId(organization.tenantId(), organization.id()).isEmpty()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Organization has departments");
        }
        referenceValidator.ensureOrganizationCanBeDeleted(organization.tenantId(), organization.id());
        organizationRepository.deleteById(organizationId);
        publishOrganizationEvent("org.organization.deleted", organization, Map.of("path", organization.path()));
    }

    public OrganizationView getOrganization(UUID organizationId) {
        return loadOrganization(organizationId).toView();
    }

    public OrganizationView getOrganization(UUID tenantId, UUID organizationId) {
        return loadOrganization(tenantId, organizationId).toView();
    }

    public List<OrganizationView> listOrganizations(UUID tenantId) {
        return organizationRepository.findByTenantId(tenantId).stream()
                .sorted(ORG_TREE_ORDER)
                .map(Organization::toView)
                .toList();
    }

    public List<OrganizationView> listChildOrganizations(UUID tenantId, UUID parentId) {
        return organizationRepository.findByParentId(tenantId, parentId).stream()
                .map(Organization::toView)
                .toList();
    }

    @Transactional
    public DepartmentView createDepartment(OrgStructureCommands.CreateDepartmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensureDepartmentCodeAvailable(command.tenantId(), command.code(), null);
        Organization organization = loadOrganization(command.tenantId(), command.organizationId());
        if (!organization.tenantId().equals(command.tenantId())) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Department tenant does not match organization");
        }
        if (organization.status() != OrgStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Organization is disabled");
        }
        Department parent = loadParentDepartment(command.parentId(), command.tenantId(), command.organizationId());
        ensureActiveParent(parent, "Parent department is disabled");
        Instant now = now();
        UUID departmentId = UUID.randomUUID();
        int level = parent == null ? 0 : parent.level() + 1;
        String basePath = parent == null ? "/" + command.organizationId() + "/" : parent.path();
        Department department = Department.create(
                departmentId,
                command.code(),
                command.name(),
                command.organizationId(),
                command.parentId(),
                level,
                appendPath(basePath, departmentId),
                command.managerId(),
                command.sortOrder(),
                command.tenantId(),
                now
        );
        Department saved = departmentRepository.save(department);
        publishDepartmentEvent("org.department.created", saved, payloadOf("parentId", saved.parentId()));
        return saved.toView();
    }

    @Transactional
    public DepartmentView updateDepartment(OrgStructureCommands.UpdateDepartmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Department department = loadDepartment(command.tenantId(), command.departmentId());
        ensureDepartmentCodeAvailable(department.tenantId(), command.code(), department.id());
        Department saved = departmentRepository.save(department.update(
                command.code(),
                command.name(),
                command.managerId(),
                command.sortOrder(),
                now()
        ));
        publishDepartmentEvent("org.department.updated", saved, payloadOf("parentId", saved.parentId()));
        return saved.toView();
    }

    @Transactional
    public DepartmentView moveDepartment(OrgStructureCommands.MoveDepartmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Department department = loadDepartment(command.tenantId(), command.departmentId());
        if (Objects.equals(department.parentId(), command.parentId())) {
            if (command.sortOrder() != null && command.sortOrder() != department.sortOrder()) {
                Department saved = departmentRepository.save(department.update(
                        department.code(),
                        department.name(),
                        department.managerId(),
                        command.sortOrder(),
                        now()
                ));
                publishDepartmentEvent(
                        "org.department.sorted",
                        saved,
                        Map.of("sortOrder", saved.sortOrder())
                );
                return saved.toView();
            }
            return department.toView();
        }
        Department parent = loadParentDepartment(
                command.parentId(),
                department.tenantId(),
                department.organizationId()
        );
        ensureActiveParent(parent, "Parent department is disabled");
        if (parent != null && parent.path().startsWith(department.path())) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Department move creates cycle");
        }
        Instant now = now();
        String oldPath = department.path();
        int oldLevel = department.level();
        int newLevel = parent == null ? 0 : parent.level() + 1;
        String basePath = parent == null ? "/" + department.organizationId() + "/" : parent.path();
        String newPath = appendPath(basePath, department.id());
        int sortOrder = command.sortOrder() == null ? department.sortOrder() : command.sortOrder();
        Department moved = department.move(command.parentId(), newLevel, newPath, sortOrder, now);
        List<Department> descendants = departmentRepository.findByPathPrefix(
                        department.tenantId(),
                        department.organizationId(),
                        oldPath
                ).stream()
                .filter(candidate -> !candidate.id().equals(department.id()))
                .toList();
        List<Department> changed = new ArrayList<>(descendants.size() + 1);
        changed.add(moved);
        int levelDelta = newLevel - oldLevel;
        for (Department descendant : descendants) {
            changed.add(descendant.move(
                    descendant.parentId(),
                    descendant.level() + levelDelta,
                    descendant.path().replaceFirst(java.util.regex.Pattern.quote(oldPath), newPath),
                    descendant.sortOrder(),
                    now
            ));
        }
        departmentRepository.saveAll(changed);
        Department saved = loadDepartment(department.tenantId(), department.id());
        publishDepartmentEvent(
                "org.department.moved",
                saved,
                payloadOf("oldPath", oldPath, "newPath", newPath, "parentId", saved.parentId())
        );
        return saved.toView();
    }

    @Transactional
    public DepartmentView activateDepartment(UUID tenantId, UUID departmentId) {
        Department saved = departmentRepository.save(loadDepartment(tenantId, departmentId).activate(now()));
        publishDepartmentEvent("org.department.enabled", saved, Map.of());
        return saved.toView();
    }

    @Transactional
    public DepartmentView disableDepartment(UUID tenantId, UUID departmentId) {
        Department saved = departmentRepository.save(loadDepartment(tenantId, departmentId).disable(now()));
        publishDepartmentEvent("org.department.disabled", saved, Map.of());
        return saved.toView();
    }

    @Transactional
    public void deleteDepartment(UUID tenantId, UUID departmentId) {
        Department department = loadDepartment(tenantId, departmentId);
        if (!departmentRepository.findByParentId(
                department.tenantId(),
                department.organizationId(),
                department.id()
        ).isEmpty()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Department has child departments");
        }
        referenceValidator.ensureDepartmentCanBeDeleted(department.tenantId(), department.id());
        departmentRepository.deleteById(departmentId);
        publishDepartmentEvent("org.department.deleted", department, Map.of("path", department.path()));
    }

    public DepartmentView getDepartment(UUID departmentId) {
        return loadDepartment(departmentId).toView();
    }

    public DepartmentView getDepartment(UUID tenantId, UUID departmentId) {
        return loadDepartment(tenantId, departmentId).toView();
    }

    public List<DepartmentView> listDepartments(UUID tenantId, UUID organizationId) {
        return departmentRepository.findByOrganizationId(tenantId, organizationId).stream()
                .sorted(DEPT_TREE_ORDER)
                .map(Department::toView)
                .toList();
    }

    public List<DepartmentView> listChildDepartments(UUID tenantId, UUID organizationId, UUID parentId) {
        return departmentRepository.findByParentId(tenantId, organizationId, parentId).stream()
                .map(Department::toView)
                .toList();
    }

    private Organization loadOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Organization not found"
                ));
    }

    private Organization loadOrganization(UUID tenantId, UUID organizationId) {
        Organization organization = loadOrganization(organizationId);
        if (!organization.tenantId().equals(tenantId)) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Organization not found");
        }
        return organization;
    }

    private Organization loadParentOrganization(UUID parentId, UUID tenantId) {
        if (parentId == null) {
            return null;
        }
        Organization parent = loadOrganization(parentId);
        if (!parent.tenantId().equals(tenantId)) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Parent organization tenant mismatch");
        }
        return parent;
    }

    private Department loadDepartment(UUID departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Department not found"
                ));
    }

    private Department loadDepartment(UUID tenantId, UUID departmentId) {
        Department department = loadDepartment(departmentId);
        if (!department.tenantId().equals(tenantId)) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Department not found");
        }
        return department;
    }

    private Department loadParentDepartment(UUID parentId, UUID tenantId, UUID organizationId) {
        if (parentId == null) {
            return null;
        }
        Department parent = loadDepartment(parentId);
        if (!parent.tenantId().equals(tenantId) || !parent.organizationId().equals(organizationId)) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Parent department scope mismatch");
        }
        return parent;
    }

    private void ensureActiveParent(Organization parent, String message) {
        if (parent != null && parent.status() != OrgStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, message);
        }
    }

    private void ensureActiveParent(Department parent, String message) {
        if (parent != null && parent.status() != DeptStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, message);
        }
    }

    private void ensureOrganizationCodeAvailable(UUID tenantId, String code, UUID currentId) {
        organizationRepository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> !existing.id().equals(currentId))
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Organization code already exists");
                });
    }

    private void ensureDepartmentCodeAvailable(UUID tenantId, String code, UUID currentId) {
        departmentRepository.findByTenantIdAndCode(tenantId, code)
                .filter(existing -> !existing.id().equals(currentId))
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Department code already exists");
                });
    }

    private Instant now() {
        return clock.instant();
    }

    private String appendPath(String parentPath, UUID nodeId) {
        String normalizedParentPath = parentPath.endsWith("/") ? parentPath : parentPath + "/";
        return normalizedParentPath + nodeId + "/";
    }

    private void publishOrganizationEvent(String eventType, Organization organization, Map<String, Object> details) {
        domainEventPublisher.publish(OrgStructureChangedEvent.of(
                eventType,
                organization.tenantId(),
                organization.id(),
                now(),
                eventPayload(details)
        ));
    }

    private void publishDepartmentEvent(String eventType, Department department, Map<String, Object> details) {
        domainEventPublisher.publish(OrgStructureChangedEvent.of(
                eventType,
                department.tenantId(),
                department.id(),
                now(),
                eventPayload(details)
        ));
    }

    private Map<String, Object> eventPayload(Map<String, Object> details) {
        TenantRequestContext context = TenantContextHolder.current().orElse(null);
        Map<String, Object> payload = new LinkedHashMap<>();
        if (context != null) {
            putIfNotNull(payload, "requestId", context.requestId());
            putIfNotNull(payload, "idempotencyKey", context.idempotencyKey());
            putIfNotNull(payload, "language", context.language().toLanguageTag());
            putIfNotNull(payload, "timezone", context.timezone().getId());
            putIfNotNull(payload, "identityAssignmentId", context.identityAssignmentId());
            putIfNotNull(payload, "identityPositionId", context.identityPositionId());
        }
        if (details != null) {
            details.forEach((key, value) -> putIfNotNull(payload, key, value));
        }
        return payload;
    }

    private void putIfNotNull(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private Map<String, Object> payloadOf(Object... keysAndValues) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            Object key = keysAndValues[i];
            if (key instanceof String name) {
                putIfNotNull(payload, name, keysAndValues[i + 1]);
            }
        }
        return payload;
    }
}
