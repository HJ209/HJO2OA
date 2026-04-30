package com.hjo2oa.todo.center.domain;

import java.util.List;

public interface TodoProcessViewRepository {

    List<InitiatedProcessSummary> findInitiated(String tenantId, String personId);

    List<DraftProcessSummary> findDrafts(String tenantId, String personId);

    List<ArchiveProcessSummary> findArchives(String tenantId, String personId);
}
