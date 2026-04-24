package com.hjo2oa.data.report.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hjo2oa.data.report.domain.DataReportRefreshedEvent;
import com.hjo2oa.data.report.domain.ReportAnalysisQuery;
import com.hjo2oa.data.report.domain.ReportRefreshMode;
import com.hjo2oa.data.report.domain.ReportRefreshTriggerMode;
import com.hjo2oa.data.report.domain.ReportStatus;
import com.hjo2oa.data.report.infrastructure.DefaultReportDataSourceRegistry;
import com.hjo2oa.data.report.infrastructure.InMemoryReportSnapshotCache;
import com.hjo2oa.data.report.support.ReportTestSupport;
import org.junit.jupiter.api.Test;

class ReportRefreshApplicationServiceTest {

    @Test
    void shouldRefreshReportAndPublishEventAndExposeSummaryTrendRanking() {
        ReportTestSupport.InMemoryReportDefinitionRepository definitionRepository =
                new ReportTestSupport.InMemoryReportDefinitionRepository();
        ReportTestSupport.InMemoryReportSnapshotRepository snapshotRepository =
                new ReportTestSupport.InMemoryReportSnapshotRepository();
        ReportTestSupport.RecordingDomainEventPublisher eventPublisher =
                new ReportTestSupport.RecordingDomainEventPublisher();
        ReportDefinitionApplicationService definitionApplicationService = new ReportDefinitionApplicationService(
                definitionRepository,
                ReportTestSupport.fixedClock()
        );
        ReportRefreshApplicationService refreshApplicationService = new ReportRefreshApplicationService(
                definitionRepository,
                snapshotRepository,
                new InMemoryReportSnapshotCache(),
                new DefaultReportDataSourceRegistry(java.util.List.of(ReportTestSupport.sampleProvider())),
                eventPublisher,
                ReportTestSupport.fixedClock()
        );
        ReportQueryApplicationService queryApplicationService = new ReportQueryApplicationService(
                definitionRepository,
                snapshotRepository,
                new InMemoryReportSnapshotCache(),
                new ReportAnalysisEngine(),
                refreshApplicationService
        );

        definitionApplicationService.create(ReportTestSupport.sampleCommand("task-pressure", ReportRefreshMode.ON_DEMAND));
        definitionApplicationService.changeStatus("task-pressure", ReportStatus.ACTIVE);

        var snapshot = refreshApplicationService.refreshByCode(
                "task-pressure",
                ReportRefreshTriggerMode.MANUAL,
                "unit-test",
                "batch-1"
        );
        assertNotNull(snapshot.id());
        assertEquals(1, eventPublisher.events().size());
        assertEquals(DataReportRefreshedEvent.EVENT_TYPE, eventPublisher.events().get(0).eventType());

        var summary = queryApplicationService.summary("task-pressure", new ReportAnalysisQuery(null, null, null, null, null, null));
        assertEquals("task-pressure", summary.reportCode());
        assertEquals("35.0", summary.metrics().get(0).value().toPlainString());

        var trend = queryApplicationService.trend("task-pressure", new ReportAnalysisQuery(null, null, null, "volume", null, null));
        assertEquals(2, trend.points().size());
        assertEquals("2026-04-23", trend.points().get(0).bucket());
        assertEquals("10.0", trend.points().get(0).value().toPlainString());

        var ranking = queryApplicationService.ranking(
                "task-pressure",
                new ReportAnalysisQuery(null, null, "organization", "volume", 10, null)
        );
        assertEquals(2, ranking.items().size());
        assertEquals("org-a", ranking.items().get(0).dimensionValue());
        assertEquals("30.0", ranking.items().get(0).value().toPlainString());

        var card = queryApplicationService.cardDataSource("task-pressure", new ReportAnalysisQuery(null, null, null, null, null, null));
        assertEquals("task-pressure", card.cardCode());
        assertEquals("volume", card.summaryMetric().metricCode());
        assertEquals(2, card.trend().size());
        assertEquals(2, card.ranking().size());
    }
}
