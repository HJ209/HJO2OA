package com.hjo2oa.data.report.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hjo2oa.data.report.domain.ReportRefreshMode;
import com.hjo2oa.data.report.domain.ReportStatus;
import com.hjo2oa.data.report.infrastructure.DefaultReportDataSourceRegistry;
import com.hjo2oa.data.report.infrastructure.InMemoryReportSnapshotCache;
import com.hjo2oa.data.report.infrastructure.ReportRefreshTriggerListener;
import com.hjo2oa.data.report.support.ReportTestSupport;
import org.junit.jupiter.api.Test;

class ReportRefreshTriggerFlowTest {

    @Test
    void shouldRefreshMatchingEventDrivenReport() {
        ReportTestSupport.InMemoryReportDefinitionRepository definitionRepository =
                new ReportTestSupport.InMemoryReportDefinitionRepository();
        ReportTestSupport.InMemoryReportSnapshotRepository snapshotRepository =
                new ReportTestSupport.InMemoryReportSnapshotRepository();
        ReportDefinitionApplicationService definitionApplicationService = new ReportDefinitionApplicationService(
                definitionRepository,
                ReportTestSupport.fixedClock()
        );
        ReportRefreshApplicationService refreshApplicationService = new ReportRefreshApplicationService(
                definitionRepository,
                snapshotRepository,
                new InMemoryReportSnapshotCache(),
                new DefaultReportDataSourceRegistry(java.util.List.of(ReportTestSupport.sampleProvider())),
                event -> {
                },
                ReportTestSupport.fixedClock()
        );
        ReportEventDrivenRefreshApplicationService eventDrivenRefreshApplicationService =
                new ReportEventDrivenRefreshApplicationService(definitionRepository, refreshApplicationService);
        ReportRefreshTriggerListener listener = new ReportRefreshTriggerListener(
                refreshApplicationService,
                eventDrivenRefreshApplicationService
        );

        definitionApplicationService.create(ReportTestSupport.sampleCommand("event-driven-report", ReportRefreshMode.EVENT_DRIVEN));
        definitionApplicationService.changeStatus("event-driven-report", ReportStatus.ACTIVE);

        listener.onDomainEvent(ReportTestSupport.simpleEvent("process.instance.completed"));

        assertTrue(snapshotRepository.findLatestReadyByReportId(
                definitionRepository.findByCode("event-driven-report").orElseThrow().id()
        ).isPresent());
    }

    @Test
    void shouldRefreshScheduledReportsWhenDue() {
        ReportTestSupport.InMemoryReportDefinitionRepository definitionRepository =
                new ReportTestSupport.InMemoryReportDefinitionRepository();
        ReportTestSupport.InMemoryReportSnapshotRepository snapshotRepository =
                new ReportTestSupport.InMemoryReportSnapshotRepository();
        ReportDefinitionApplicationService definitionApplicationService = new ReportDefinitionApplicationService(
                definitionRepository,
                ReportTestSupport.fixedClock()
        );
        ReportRefreshApplicationService refreshApplicationService = new ReportRefreshApplicationService(
                definitionRepository,
                snapshotRepository,
                new InMemoryReportSnapshotCache(),
                new DefaultReportDataSourceRegistry(java.util.List.of(ReportTestSupport.sampleProvider())),
                event -> {
                },
                ReportTestSupport.clockAt(ReportTestSupport.FIXED_TIME.plusSeconds(301))
        );

        definitionApplicationService.create(ReportTestSupport.sampleCommand("scheduled-report", ReportRefreshMode.SCHEDULED));
        definitionApplicationService.changeStatus("scheduled-report", ReportStatus.ACTIVE);

        int refreshedCount = refreshApplicationService.refreshDueScheduledReports("infra.scheduler.tick");

        assertEquals(1, refreshedCount);
        assertTrue(snapshotRepository.findLatestReadyByReportId(
                definitionRepository.findByCode("scheduled-report").orElseThrow().id()
        ).isPresent());
    }
}
