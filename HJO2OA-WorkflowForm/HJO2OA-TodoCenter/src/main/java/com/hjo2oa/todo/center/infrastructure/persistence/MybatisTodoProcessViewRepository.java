package com.hjo2oa.todo.center.infrastructure.persistence;

import com.hjo2oa.todo.center.domain.ArchiveProcessSummary;
import com.hjo2oa.todo.center.domain.DraftProcessSummary;
import com.hjo2oa.todo.center.domain.InitiatedProcessSummary;
import com.hjo2oa.todo.center.domain.TodoProcessViewRepository;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class MybatisTodoProcessViewRepository implements TodoProcessViewRepository {

    private final TodoProcessViewMapper mapper;

    public MybatisTodoProcessViewRepository(TodoProcessViewMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<InitiatedProcessSummary> findInitiated(String tenantId, String personId) {
        return mapper.findInitiated(tenantId, personId).stream()
                .map(row -> new InitiatedProcessSummary(
                        row.getInstanceId(),
                        row.getDefinitionId(),
                        row.getDefinitionCode(),
                        row.getTitle(),
                        row.getCategory(),
                        row.getStatus(),
                        row.getStartTime(),
                        row.getEndTime(),
                        row.getUpdatedAt()
                ))
                .toList();
    }

    @Override
    public List<DraftProcessSummary> findDrafts(String tenantId, String personId) {
        return mapper.findDrafts(tenantId, personId).stream()
                .map(row -> new DraftProcessSummary(
                        row.getSubmissionId(),
                        row.getMetadataId(),
                        row.getMetadataCode(),
                        row.getMetadataVersion(),
                        row.getProcessInstanceId(),
                        row.getNodeId(),
                        row.getCreatedAt(),
                        row.getUpdatedAt()
                ))
                .toList();
    }

    @Override
    public List<ArchiveProcessSummary> findArchives(String tenantId, String personId) {
        return mapper.findArchives(tenantId, personId).stream()
                .map(row -> new ArchiveProcessSummary(
                        row.getInstanceId(),
                        row.getDefinitionId(),
                        row.getDefinitionCode(),
                        row.getTitle(),
                        row.getCategory(),
                        row.getStatus(),
                        row.getStartTime(),
                        row.getEndTime(),
                        row.getUpdatedAt()
                ))
                .toList();
    }
}
