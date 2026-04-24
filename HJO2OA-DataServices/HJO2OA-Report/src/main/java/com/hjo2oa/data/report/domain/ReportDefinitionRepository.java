package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReportDefinitionRepository {

    Optional<ReportDefinition> findByCode(String code);

    Optional<ReportDefinition> findById(String id);

    ReportDefinition save(ReportDefinition reportDefinition);

    ReportDefinitionPage page(ReportDefinitionQuery query);

    List<ReportDefinition> findByRefreshModeAndStatus(ReportRefreshMode refreshMode, ReportStatus status);

    List<ReportDefinition> findDueScheduledReports(Instant now);
}
