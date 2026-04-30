package com.hjo2oa.org.org.structure.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.org.org.structure.domain.Department;
import com.hjo2oa.org.org.structure.domain.DepartmentRepository;
import com.hjo2oa.org.org.structure.domain.Organization;
import com.hjo2oa.org.org.structure.domain.OrganizationRepository;
import com.hjo2oa.org.org.structure.domain.OrganizationView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrgStructureApplicationServiceTest {

    private static final UUID TENANT_A = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private InMemoryOrganizationRepository organizations;
    private OrgStructureApplicationService service;

    @BeforeEach
    void setUp() {
        organizations = new InMemoryOrganizationRepository();
        service = new OrgStructureApplicationService(
                organizations,
                new InMemoryDepartmentRepository(),
                CLOCK,
                new RecordingPublisher(),
                OrgStructureReferenceValidator.NOOP
        );
    }

    @Test
    void movingOrganizationUpdatesDescendantPathsAndSortOrder() {
        OrganizationView root = createOrganization("ROOT", null, 0);
        OrganizationView child = createOrganization("CHILD", root.id(), 10);
        OrganizationView grandchild = createOrganization("GRAND", child.id(), 20);

        OrganizationView moved = service.moveOrganization(
                new OrgStructureCommands.MoveOrganizationCommand(child.id(), null, 5, TENANT_A)
        );

        Organization changedGrandchild = organizations.findById(grandchild.id()).orElseThrow();
        assertThat(moved.parentId()).isNull();
        assertThat(moved.level()).isZero();
        assertThat(moved.sortOrder()).isEqualTo(5);
        assertThat(changedGrandchild.level()).isEqualTo(1);
        assertThat(changedGrandchild.path()).startsWith(moved.path());
    }

    @Test
    void movingOrganizationUnderDescendantIsRejected() {
        OrganizationView root = createOrganization("ROOT", null, 0);
        OrganizationView child = createOrganization("CHILD", root.id(), 10);
        OrganizationView grandchild = createOrganization("GRAND", child.id(), 20);

        assertThatThrownBy(() -> service.moveOrganization(
                new OrgStructureCommands.MoveOrganizationCommand(root.id(), grandchild.id(), null, TENANT_A)
        )).isInstanceOf(BizException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void organizationUpdateIsScopedByTenant() {
        OrganizationView root = createOrganization("ROOT", null, 0);

        assertThatThrownBy(() -> service.updateOrganization(new OrgStructureCommands.UpdateOrganizationCommand(
                root.id(),
                "ROOT2",
                "Root 2",
                null,
                "COMPANY",
                1,
                TENANT_B
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("not found");
    }

    private OrganizationView createOrganization(String code, UUID parentId, int sortOrder) {
        return service.createOrganization(new OrgStructureCommands.CreateOrganizationCommand(
                code,
                code + " Name",
                null,
                "COMPANY",
                parentId,
                sortOrder,
                TENANT_A
        ));
    }

    private static final class InMemoryOrganizationRepository implements OrganizationRepository {

        private final Map<UUID, Organization> rows = new HashMap<>();

        @Override
        public Optional<Organization> findById(UUID organizationId) {
            return Optional.ofNullable(rows.get(organizationId));
        }

        @Override
        public Optional<Organization> findByTenantIdAndCode(UUID tenantId, String code) {
            return rows.values().stream()
                    .filter(row -> row.tenantId().equals(tenantId))
                    .filter(row -> row.code().equals(code))
                    .findFirst();
        }

        @Override
        public List<Organization> findByTenantId(UUID tenantId) {
            return rows.values().stream()
                    .filter(row -> row.tenantId().equals(tenantId))
                    .sorted(Comparator.comparing(Organization::code))
                    .toList();
        }

        @Override
        public List<Organization> findByParentId(UUID tenantId, UUID parentId) {
            return rows.values().stream()
                    .filter(row -> row.tenantId().equals(tenantId))
                    .filter(row -> java.util.Objects.equals(row.parentId(), parentId))
                    .toList();
        }

        @Override
        public List<Organization> findByPathPrefix(UUID tenantId, String pathPrefix) {
            return rows.values().stream()
                    .filter(row -> row.tenantId().equals(tenantId))
                    .filter(row -> row.path().startsWith(pathPrefix))
                    .toList();
        }

        @Override
        public Organization save(Organization organization) {
            rows.put(organization.id(), organization);
            return organization;
        }

        @Override
        public List<Organization> saveAll(List<Organization> organizations) {
            organizations.forEach(this::save);
            return new ArrayList<>(organizations);
        }

        @Override
        public void deleteById(UUID organizationId) {
            rows.remove(organizationId);
        }
    }

    private static final class InMemoryDepartmentRepository implements DepartmentRepository {

        @Override
        public Optional<Department> findById(UUID departmentId) {
            return Optional.empty();
        }

        @Override
        public Optional<Department> findByTenantIdAndCode(UUID tenantId, String code) {
            return Optional.empty();
        }

        @Override
        public List<Department> findByOrganizationId(UUID tenantId, UUID organizationId) {
            return List.of();
        }

        @Override
        public List<Department> findByParentId(UUID tenantId, UUID organizationId, UUID parentId) {
            return List.of();
        }

        @Override
        public List<Department> findByPathPrefix(UUID tenantId, UUID organizationId, String pathPrefix) {
            return List.of();
        }

        @Override
        public Department save(Department department) {
            return department;
        }

        @Override
        public List<Department> saveAll(List<Department> departments) {
            return departments;
        }

        @Override
        public void deleteById(UUID departmentId) {
        }
    }

    private static final class RecordingPublisher implements com.hjo2oa.shared.messaging.DomainEventPublisher {

        @Override
        public void publish(DomainEvent event) {
        }
    }
}
