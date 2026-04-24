package com.hjo2oa.data.report.application;

import com.hjo2oa.data.report.domain.ReportDefinition;
import com.hjo2oa.data.report.domain.ReportDefinitionRepository;
import com.hjo2oa.data.report.domain.ReportRefreshMode;
import com.hjo2oa.data.report.domain.ReportRefreshTriggerMode;
import com.hjo2oa.data.report.domain.ReportStatus;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReportEventDrivenRefreshApplicationService {

    private final ReportDefinitionRepository reportDefinitionRepository;
    private final ReportRefreshApplicationService reportRefreshApplicationService;

    public ReportEventDrivenRefreshApplicationService(
            ReportDefinitionRepository reportDefinitionRepository,
            ReportRefreshApplicationService reportRefreshApplicationService
    ) {
        this.reportDefinitionRepository = Objects.requireNonNull(
                reportDefinitionRepository,
                "reportDefinitionRepository must not be null");
        this.reportRefreshApplicationService = Objects.requireNonNull(
                reportRefreshApplicationService,
                "reportRefreshApplicationService must not be null");
    }

    public int refreshForDomainEvent(DomainEvent event) {
        List<ReportDefinition> candidates = reportDefinitionRepository.findByRefreshModeAndStatus(
                ReportRefreshMode.EVENT_DRIVEN,
                ReportStatus.ACTIVE
        );
        int refreshedCount = 0;
        for (ReportDefinition candidate : candidates) {
            if (!Objects.equals(candidate.tenantId(), event.tenantId())) {
                continue;
            }
            if (!candidate.caliberDefinition().matchesEvent(event.eventType())) {
                continue;
            }
            try {
                reportRefreshApplicationService.refreshByCode(
                        candidate.code(),
                        ReportRefreshTriggerMode.EVENT_DRIVEN,
                        event.eventType(),
                        event.eventId().toString()
                );
                refreshedCount++;
            } catch (RuntimeException ignored) {
                // Preserve failure snapshot and continue evaluating other reports.
            }
        }
        return refreshedCount;
    }
}
