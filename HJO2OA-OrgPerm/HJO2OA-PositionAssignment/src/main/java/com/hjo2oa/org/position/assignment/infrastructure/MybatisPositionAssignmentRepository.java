package com.hjo2oa.org.position.assignment.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.position.assignment.domain.Assignment;
import com.hjo2oa.org.position.assignment.domain.AssignmentStatus;
import com.hjo2oa.org.position.assignment.domain.AssignmentType;
import com.hjo2oa.org.position.assignment.domain.Position;
import com.hjo2oa.org.position.assignment.domain.PositionAssignmentRepository;
import com.hjo2oa.org.position.assignment.domain.PositionCategory;
import com.hjo2oa.org.position.assignment.domain.PositionRole;
import com.hjo2oa.org.position.assignment.domain.PositionStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisPositionAssignmentRepository implements PositionAssignmentRepository {

    private final PositionMapper positionMapper;
    private final AssignmentMapper assignmentMapper;
    private final PositionRoleMapper positionRoleMapper;

    public MybatisPositionAssignmentRepository(
            PositionMapper positionMapper,
            AssignmentMapper assignmentMapper,
            PositionRoleMapper positionRoleMapper
    ) {
        this.positionMapper = Objects.requireNonNull(positionMapper, "positionMapper must not be null");
        this.assignmentMapper = Objects.requireNonNull(assignmentMapper, "assignmentMapper must not be null");
        this.positionRoleMapper = Objects.requireNonNull(positionRoleMapper, "positionRoleMapper must not be null");
    }

    @Override
    public Optional<Position> findPositionById(UUID positionId) {
        return Optional.ofNullable(positionMapper.selectById(positionId)).map(this::toPosition);
    }

    @Override
    public Optional<Position> findPositionByCode(UUID tenantId, String code) {
        return Optional.ofNullable(positionMapper.selectOne(Wrappers.<PositionEntity>lambdaQuery()
                .eq(PositionEntity::getTenantId, tenantId)
                .eq(PositionEntity::getCode, code)))
                .map(this::toPosition);
    }

    @Override
    public List<Position> findPositions(UUID tenantId, UUID organizationId, UUID departmentId) {
        var wrapper = Wrappers.<PositionEntity>lambdaQuery()
                .eq(PositionEntity::getTenantId, tenantId)
                .orderByAsc(PositionEntity::getSortOrder)
                .orderByAsc(PositionEntity::getCode);
        if (organizationId != null) {
            wrapper.eq(PositionEntity::getOrganizationId, organizationId);
        }
        if (departmentId != null) {
            wrapper.eq(PositionEntity::getDepartmentId, departmentId);
        }
        return positionMapper.selectList(wrapper).stream().map(this::toPosition).toList();
    }

    @Override
    public Position savePosition(Position position) {
        PositionEntity existing = positionMapper.selectById(position.id());
        PositionEntity entity = toPositionEntity(position, existing);
        if (existing == null) {
            positionMapper.insert(entity);
        } else {
            positionMapper.updateById(entity);
        }
        return toPosition(positionMapper.selectById(position.id()));
    }

    @Override
    public Optional<Assignment> findAssignmentById(UUID assignmentId) {
        return Optional.ofNullable(assignmentMapper.selectById(assignmentId)).map(this::toAssignment);
    }

    @Override
    public List<Assignment> findAssignmentsByPerson(UUID tenantId, UUID personId) {
        return assignmentMapper.selectList(Wrappers.<AssignmentEntity>lambdaQuery()
                        .eq(AssignmentEntity::getTenantId, tenantId)
                        .eq(AssignmentEntity::getPersonId, personId)
                        .orderByAsc(AssignmentEntity::getType)
                        .orderByAsc(AssignmentEntity::getCreatedAt))
                .stream()
                .map(this::toAssignment)
                .toList();
    }

    @Override
    public List<Assignment> findAssignmentsByPosition(UUID tenantId, UUID positionId) {
        return assignmentMapper.selectList(Wrappers.<AssignmentEntity>lambdaQuery()
                        .eq(AssignmentEntity::getTenantId, tenantId)
                        .eq(AssignmentEntity::getPositionId, positionId)
                        .orderByAsc(AssignmentEntity::getPersonId))
                .stream()
                .map(this::toAssignment)
                .toList();
    }

    @Override
    public Optional<Assignment> findActiveAssignment(UUID tenantId, UUID personId, UUID positionId) {
        return Optional.ofNullable(assignmentMapper.selectOne(Wrappers.<AssignmentEntity>lambdaQuery()
                        .eq(AssignmentEntity::getTenantId, tenantId)
                        .eq(AssignmentEntity::getPersonId, personId)
                        .eq(AssignmentEntity::getPositionId, positionId)
                        .eq(AssignmentEntity::getStatus, AssignmentStatus.ACTIVE.name())))
                .map(this::toAssignment);
    }

    @Override
    public Optional<Assignment> findActivePrimaryAssignment(UUID tenantId, UUID personId) {
        return Optional.ofNullable(assignmentMapper.selectOne(Wrappers.<AssignmentEntity>lambdaQuery()
                        .eq(AssignmentEntity::getTenantId, tenantId)
                        .eq(AssignmentEntity::getPersonId, personId)
                        .eq(AssignmentEntity::getType, AssignmentType.PRIMARY.name())
                        .eq(AssignmentEntity::getStatus, AssignmentStatus.ACTIVE.name())))
                .map(this::toAssignment);
    }

    @Override
    public Assignment saveAssignment(Assignment assignment) {
        AssignmentEntity existing = assignmentMapper.selectById(assignment.id());
        AssignmentEntity entity = toAssignmentEntity(assignment, existing);
        if (existing == null) {
            assignmentMapper.insert(entity);
        } else {
            assignmentMapper.updateById(entity);
        }
        return toAssignment(assignmentMapper.selectById(assignment.id()));
    }

    @Override
    public List<PositionRole> findRolesByPosition(UUID tenantId, UUID positionId) {
        return positionRoleMapper.selectList(Wrappers.<PositionRoleEntity>lambdaQuery()
                        .eq(PositionRoleEntity::getTenantId, tenantId)
                        .eq(PositionRoleEntity::getPositionId, positionId)
                        .orderByAsc(PositionRoleEntity::getRoleId))
                .stream()
                .map(this::toPositionRole)
                .toList();
    }

    @Override
    public Optional<PositionRole> findPositionRole(UUID tenantId, UUID positionId, UUID roleId) {
        return Optional.ofNullable(positionRoleMapper.selectOne(Wrappers.<PositionRoleEntity>lambdaQuery()
                        .eq(PositionRoleEntity::getTenantId, tenantId)
                        .eq(PositionRoleEntity::getPositionId, positionId)
                        .eq(PositionRoleEntity::getRoleId, roleId)))
                .map(this::toPositionRole);
    }

    @Override
    public PositionRole savePositionRole(PositionRole positionRole) {
        PositionRoleEntity existing = positionRoleMapper.selectById(positionRole.id());
        PositionRoleEntity entity = toPositionRoleEntity(positionRole, existing);
        if (existing == null) {
            positionRoleMapper.insert(entity);
        } else {
            positionRoleMapper.updateById(entity);
        }
        return toPositionRole(positionRoleMapper.selectById(positionRole.id()));
    }

    @Override
    public void deletePositionRole(UUID positionRoleId) {
        positionRoleMapper.deleteById(positionRoleId);
    }

    private Position toPosition(PositionEntity entity) {
        return new Position(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getOrganizationId(),
                entity.getDepartmentId(),
                PositionCategory.valueOf(entity.getCategory()),
                entity.getLevel(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder(),
                PositionStatus.valueOf(entity.getStatus()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private Assignment toAssignment(AssignmentEntity entity) {
        return new Assignment(
                entity.getId(),
                entity.getPersonId(),
                entity.getPositionId(),
                AssignmentType.valueOf(entity.getType()),
                entity.getStartDate(),
                entity.getEndDate(),
                AssignmentStatus.valueOf(entity.getStatus()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PositionRole toPositionRole(PositionRoleEntity entity) {
        return new PositionRole(
                entity.getId(),
                entity.getPositionId(),
                entity.getRoleId(),
                entity.getTenantId(),
                entity.getCreatedAt()
        );
    }

    private PositionEntity toPositionEntity(Position position, PositionEntity existing) {
        PositionEntity entity = existing == null ? new PositionEntity() : existing;
        entity.setId(position.id());
        entity.setCode(position.code());
        entity.setName(position.name());
        entity.setOrganizationId(position.organizationId());
        entity.setDepartmentId(position.departmentId());
        entity.setCategory(position.category().name());
        entity.setLevel(position.level());
        entity.setSortOrder(position.sortOrder());
        entity.setStatus(position.status().name());
        entity.setTenantId(position.tenantId());
        entity.setCreatedAt(position.createdAt());
        entity.setUpdatedAt(position.updatedAt());
        return entity;
    }

    private AssignmentEntity toAssignmentEntity(Assignment assignment, AssignmentEntity existing) {
        AssignmentEntity entity = existing == null ? new AssignmentEntity() : existing;
        entity.setId(assignment.id());
        entity.setPersonId(assignment.personId());
        entity.setPositionId(assignment.positionId());
        entity.setType(assignment.type().name());
        entity.setStartDate(assignment.startDate());
        entity.setEndDate(assignment.endDate());
        entity.setStatus(assignment.status().name());
        entity.setTenantId(assignment.tenantId());
        entity.setCreatedAt(assignment.createdAt());
        entity.setUpdatedAt(assignment.updatedAt());
        return entity;
    }

    private PositionRoleEntity toPositionRoleEntity(PositionRole positionRole, PositionRoleEntity existing) {
        PositionRoleEntity entity = existing == null ? new PositionRoleEntity() : existing;
        entity.setId(positionRole.id());
        entity.setPositionId(positionRole.positionId());
        entity.setRoleId(positionRole.roleId());
        entity.setTenantId(positionRole.tenantId());
        entity.setCreatedAt(positionRole.createdAt());
        return entity;
    }
}
