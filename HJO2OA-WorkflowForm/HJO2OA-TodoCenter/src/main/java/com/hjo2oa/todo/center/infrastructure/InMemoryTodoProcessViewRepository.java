package com.hjo2oa.todo.center.infrastructure;

import com.hjo2oa.todo.center.domain.ArchiveProcessSummary;
import com.hjo2oa.todo.center.domain.DraftProcessSummary;
import com.hjo2oa.todo.center.domain.InitiatedProcessSummary;
import com.hjo2oa.todo.center.domain.TodoProcessViewRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTodoProcessViewRepository implements TodoProcessViewRepository {

    private final Map<String, CopyOnWriteArrayList<InitiatedProcessSummary>> initiatedByOwner =
            new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<DraftProcessSummary>> draftsByOwner =
            new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<ArchiveProcessSummary>> archivesByOwner =
            new ConcurrentHashMap<>();

    @Override
    public List<InitiatedProcessSummary> findInitiated(String tenantId, String personId) {
        return List.copyOf(initiatedByOwner.getOrDefault(ownerKey(tenantId, personId), new CopyOnWriteArrayList<>()));
    }

    @Override
    public List<DraftProcessSummary> findDrafts(String tenantId, String personId) {
        return List.copyOf(draftsByOwner.getOrDefault(ownerKey(tenantId, personId), new CopyOnWriteArrayList<>()));
    }

    @Override
    public List<ArchiveProcessSummary> findArchives(String tenantId, String personId) {
        return List.copyOf(archivesByOwner.getOrDefault(ownerKey(tenantId, personId), new CopyOnWriteArrayList<>()));
    }

    public InitiatedProcessSummary saveInitiated(
            String tenantId,
            String personId,
            InitiatedProcessSummary summary
    ) {
        initiatedByOwner.computeIfAbsent(ownerKey(tenantId, personId), ignored -> new CopyOnWriteArrayList<>())
                .add(summary);
        return summary;
    }

    public DraftProcessSummary saveDraft(String tenantId, String personId, DraftProcessSummary summary) {
        draftsByOwner.computeIfAbsent(ownerKey(tenantId, personId), ignored -> new CopyOnWriteArrayList<>())
                .add(summary);
        return summary;
    }

    public ArchiveProcessSummary saveArchive(String tenantId, String personId, ArchiveProcessSummary summary) {
        archivesByOwner.computeIfAbsent(ownerKey(tenantId, personId), ignored -> new CopyOnWriteArrayList<>())
                .add(summary);
        return summary;
    }

    private String ownerKey(String tenantId, String personId) {
        return tenantId + "\u0000" + personId;
    }
}
