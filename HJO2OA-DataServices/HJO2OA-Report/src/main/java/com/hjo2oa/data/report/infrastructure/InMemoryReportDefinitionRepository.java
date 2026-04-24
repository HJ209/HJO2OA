package com.hjo2oa.data.report.infrastructure;

import com.hjo2oa.data.report.domain.ReportDefinition;
import com.hjo2oa.data.report.domain.ReportDefinitionPage;
import com.hjo2oa.data.report.domain.ReportDefinitionQuery;
import com.hjo2oa.data.report.domain.ReportDefinitionRepository;
import com.hjo2oa.data.report.domain.ReportRefreshMode;
import com.hjo2oa.data.report.domain.ReportStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryReportDefinitionRepository implements ReportDefinitionRepository {

    private final Map<String, ReportDefinition> definitionsByCode = new LinkedHashMap<>();
    private final Map<String, ReportDefinition> definitionsById = new LinkedHashMap<>();

    @Override
    public Optional<ReportDefinition> findByCode(String code) {
        return Optional.ofNullable(definitionsByCode.get(code));
    }

    @Override
    public Optional<ReportDefinition> findById(String id) {
        return Optional.ofNullable(definitionsById.get(id));
    }

    @Override
    public ReportDefinition save(ReportDefinition reportDefinition) {
        String reportId = reportDefinition.id() == null ? UUID.randomUUID().toString() : reportDefinition.id();
        ReportDefinition persisted = new ReportDefinition(
                reportId,
                reportDefinition.code(),
                reportDefinition.name(),
                reportDefinition.reportType(),
                reportDefinition.sourceScope(),
                reportDefinition.refreshMode(),
                reportDefinition.visibilityMode(),
                reportDefinition.status(),
                reportDefinition.tenantId(),
                reportDefinition.definitionVersion(),
                reportDefinition.caliberDefinition(),
                reportDefinition.refreshConfig(),
                reportDefinition.cardProtocol(),
                reportDefinition.lastRefreshedAt(),
                reportDefinition.lastFreshnessStatus(),
                reportDefinition.lastRefreshBatch(),
                reportDefinition.nextRefreshAt(),
                reportDefinition.createdAt(),
                reportDefinition.updatedAt(),
                reportDefinition.metrics(),
                reportDefinition.dimensions()
        );
        definitionsByCode.put(persisted.code(), persisted);
        definitionsById.put(persisted.id(), persisted);
        return persisted;
    }

    @Override
    public ReportDefinitionPage page(ReportDefinitionQuery query) {
        List<ReportDefinition> items = definitionsByCode.values().stream()
                .filter(item -> query.reportType() == null || item.reportType() == query.reportType())
                .filter(item -> query.sourceScope() == null || item.sourceScope() == query.sourceScope())
                .filter(item -> query.status() == null || item.status() == query.status())
                .filter(item -> query.visibilityMode() == null || item.visibilityMode() == query.visibilityMode())
                .sorted(Comparator.comparing(ReportDefinition::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int fromIndex = Math.min((query.page() - 1) * query.size(), items.size());
        int toIndex = Math.min(fromIndex + query.size(), items.size());
        return new ReportDefinitionPage(items.subList(fromIndex, toIndex), items.size());
    }

    @Override
    public List<ReportDefinition> findByRefreshModeAndStatus(ReportRefreshMode refreshMode, ReportStatus status) {
        return definitionsByCode.values().stream()
                .filter(item -> item.refreshMode() == refreshMode && item.status() == status)
                .toList();
    }

    @Override
    public List<ReportDefinition> findDueScheduledReports(Instant now) {
        return definitionsByCode.values().stream()
                .filter(item -> item.dueForScheduledRefresh(now))
                .toList();
    }
}
