package com.hjo2oa.todo.center.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hjo2oa.todo.center.domain.CopiedTodoItem;
import com.hjo2oa.todo.center.domain.CopiedTodoReadStatus;
import com.hjo2oa.todo.center.domain.CopiedTodoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisCopiedTodoRepository implements CopiedTodoRepository {

    private final CopiedTodoMapper mapper;

    public MybatisCopiedTodoRepository(CopiedTodoMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<CopiedTodoItem> findByTodoId(String todoId) {
        return Optional.ofNullable(mapper.selectById(todoId)).map(this::toDomain);
    }

    @Override
    public CopiedTodoItem save(CopiedTodoItem copiedTodoItem) {
        CopiedTodoEntity existing = mapper.selectById(copiedTodoItem.todoId());
        CopiedTodoEntity entity = toEntity(copiedTodoItem, existing);
        if (existing == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return findByTodoId(copiedTodoItem.todoId()).orElseThrow();
    }

    @Override
    public List<CopiedTodoItem> findByRecipientAssignmentId(String recipientAssignmentId) {
        return mapper.selectList(new QueryWrapper<CopiedTodoEntity>()
                        .eq("recipient_assignment_id", recipientAssignmentId)
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CopiedTodoItem> findByTenantIdAndRecipientAssignmentId(
            String tenantId,
            String recipientAssignmentId
    ) {
        return mapper.selectList(new QueryWrapper<CopiedTodoEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("recipient_assignment_id", recipientAssignmentId)
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countUnreadByRecipientAssignmentId(String recipientAssignmentId) {
        return mapper.selectCount(new QueryWrapper<CopiedTodoEntity>()
                .eq("recipient_assignment_id", recipientAssignmentId)
                .eq("read_status", CopiedTodoReadStatus.UNREAD.name()));
    }

    @Override
    public long countUnreadByTenantIdAndRecipientAssignmentId(String tenantId, String recipientAssignmentId) {
        return mapper.selectCount(new QueryWrapper<CopiedTodoEntity>()
                .eq("tenant_id", tenantId)
                .eq("recipient_assignment_id", recipientAssignmentId)
                .eq("read_status", CopiedTodoReadStatus.UNREAD.name()));
    }

    private CopiedTodoItem toDomain(CopiedTodoEntity entity) {
        return new CopiedTodoItem(
                entity.getTodoId(),
                entity.getTaskId(),
                entity.getInstanceId(),
                entity.getTenantId(),
                entity.getRecipientAssignmentId(),
                entity.getType(),
                entity.getCategory(),
                entity.getTitle(),
                entity.getUrgency(),
                CopiedTodoReadStatus.valueOf(entity.getReadStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getReadAt()
        );
    }

    private CopiedTodoEntity toEntity(CopiedTodoItem copiedTodoItem, CopiedTodoEntity existing) {
        CopiedTodoEntity entity = existing == null ? new CopiedTodoEntity() : existing;
        entity.setTodoId(copiedTodoItem.todoId());
        entity.setTaskId(copiedTodoItem.taskId());
        entity.setInstanceId(copiedTodoItem.instanceId());
        entity.setTenantId(copiedTodoItem.tenantId());
        entity.setRecipientAssignmentId(copiedTodoItem.recipientAssignmentId());
        entity.setType(copiedTodoItem.type());
        entity.setCategory(copiedTodoItem.category());
        entity.setTitle(copiedTodoItem.title());
        entity.setUrgency(copiedTodoItem.urgency());
        entity.setReadStatus(copiedTodoItem.readStatus().name());
        entity.setCreatedAt(copiedTodoItem.createdAt());
        entity.setUpdatedAt(copiedTodoItem.updatedAt());
        entity.setReadAt(copiedTodoItem.readAt());
        return entity;
    }
}
