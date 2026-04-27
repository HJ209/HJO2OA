package com.hjo2oa.org.org.structure.application;

import com.hjo2oa.org.org.structure.domain.Department;
import com.hjo2oa.org.org.structure.domain.DepartmentRepository;
import com.hjo2oa.org.org.structure.domain.DepartmentView;
import com.hjo2oa.org.org.structure.domain.Organization;
import com.hjo2oa.org.org.structure.domain.OrganizationRepository;
import com.hjo2oa.org.org.structure.domain.OrganizationView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
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

    public OrgStructureApplicationService(
            OrganizationRepository organizationRepository,
            DepartmentRepository departmentRepository
    ) {
        this(organizationRepository, departmentRepository, Clock.systemUTC());
    }

    public OrgStructureApplicationService(
            OrganizationRepository organizationRepository,
            DepartmentRepository departmentRepository,
            Clock clock
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
    }

    @Transactional
    public OrganizationView createOrganization(OrgStructureCommands.CreateOrganizationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensureOrganizationCodeAvailable(command.tenantId(), command.code(), null);
        Organization parent = loadParentOrganization(command.parentId(), command.tenantId());
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
        return organizationRepository.save(organization).toView();
    }

    @Transactional
    public OrganizationView updateOrganization(OrgStructureCommands.UpdateOrganizationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Organization organization = loadOrganization(command.organizationId());
        ensureOrganizationCodeAvailable(organization.tenantId(), command.code(), organization.id());
        return organizationRepository.save(organization.update(
                command.code(),
                command.name(),
                command.shortName(),
                command.type(),
                command.sortOrder(),
                now()
        )).toView();
    }

    @Transactional
    public OrganizationView moveOrganization(OrgStructureCommands.MoveOrganizationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Organization organization = loadOrganization(command.organizationId());
        if (Objects.equals(organization.parentId(), command.parentId())) {
            return organization.toView();
        }
        Organization parent = loadParentOrganization(command.parentId(), organization.tenantId());
        if (parent != null && parent.path().startsWith(organization.path())) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Organization move creates cycle");
        }
        Instant now = now();
        String oldPath = organization.path();
        int oldLevel = organization.level();
        int newLevel = parent == null ? 0 : parent.level() + 1;
        String newPath = appendPath(parent == null ? "/" : parent.path(), organization.id());
        Organization moved = organization.move(command.parentId(), newLevel, newPath, now);
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
                    now
            ));
        }
        organizationRepository.saveAll(changed);
        return loadOrganization(organization.id()).toView();
    }

    @Transactional
    public OrganizationView activateOrganization(UUID organizationId) {
        return organizationRepository.save(loadOrganization(organizationId).activate(now())).toView();
    }

    @Transactional
    public OrganizationView disableOrganization(UUID organizationId) {
        return organizationRepository.save(loadOrganization(organizationId).disable(now())).toView();
    }

    @Transactional
    public void deleteOrganization(UUID organizationId) {
        Organization organization = loadOrganization(organizationId);
        if (!organizationRepository.findByParentId(organization.tenantId(), organization.id()).isEmpty()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Organization has child organizations");
        }
        if (!departmentRepository.findByOrganizationId(organization.tenantId(), organization.id()).isEmpty()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Organization has departments");
        }
        organizationRepository.deleteById(organizationId);
    }

    public OrganizationView getOrganization(UUID organizationId) {
        return loadOrganization(organizationId).toView();
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
        Organization organization = loadOrganization(command.organizationId());
        if (!organization.tenantId().equals(command.tenantId())) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Department tenant does not match organization");
        }
        Department parent = loadParentDepartment(command.parentId(), command.tenantId(), command.organizationId());
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
        return departmentRepository.save(department).toView();
    }

    @Transactional
    public DepartmentView updateDepartment(OrgStructureCommands.UpdateDepartmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Department department = loadDepartment(command.departmentId());
        ensureDepartmentCodeAvailable(department.tenantId(), command.code(), department.id());
        return departmentRepository.save(department.update(
                command.code(),
                command.name(),
                command.managerId(),
                command.sortOrder(),
                now()
        )).toView();
    }

    @Transactional
    public DepartmentView moveDepartment(OrgStructureCommands.MoveDepartmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Department department = loadDepartment(command.departmentId());
        if (Objects.equals(department.parentId(), command.parentId())) {
            return department.toView();
        }
        Department parent = loadParentDepartment(
                command.parentId(),
                department.tenantId(),
                department.organizationId()
        );
        if (parent != null && parent.path().startsWith(department.path())) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Department move creates cycle");
        }
        Instant now = now();
        String oldPath = department.path();
        int oldLevel = department.level();
        int newLevel = parent == null ? 0 : parent.level() + 1;
        String basePath = parent == null ? "/" + department.organizationId() + "/" : parent.path();
        String newPath = appendPath(basePath, department.id());
        Department moved = department.move(command.parentId(), newLevel, newPath, now);
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
                    now
            ));
        }
        departmentRepository.saveAll(changed);
        return loadDepartment(department.id()).toView();
    }

    @Transactional
    public DepartmentView activateDepartment(UUID departmentId) {
        return departmentRepository.save(loadDepartment(departmentId).activate(now())).toView();
    }

    @Transactional
    public DepartmentView disableDepartment(UUID departmentId) {
        return departmentRepository.save(loadDepartment(departmentId).disable(now())).toView();
    }

    @Transactional
    public void deleteDepartment(UUID departmentId) {
        Department department = loadDepartment(departmentId);
        if (!departmentRepository.findByParentId(
                department.tenantId(),
                department.organizationId(),
                department.id()
        ).isEmpty()) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Department has child departments");
        }
        departmentRepository.deleteById(departmentId);
    }

    public DepartmentView getDepartment(UUID departmentId) {
        return loadDepartment(departmentId).toView();
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
}
