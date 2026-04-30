package com.hjo2oa.data.report.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hjo2oa.data.report.application.ReportAnalysisEngine;
import com.hjo2oa.data.report.application.ReportDefinitionApplicationService;
import com.hjo2oa.data.report.application.ReportEventDrivenRefreshApplicationService;
import com.hjo2oa.data.report.application.ReportQueryApplicationService;
import com.hjo2oa.data.report.application.ReportRefreshApplicationService;
import com.hjo2oa.data.report.infrastructure.DefaultReportDataSourceRegistry;
import com.hjo2oa.data.report.infrastructure.InMemoryReportSnapshotCache;
import com.hjo2oa.data.report.support.ReportTestSupport;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReportControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldExposeDefinitionAndCardDataSourceUsingSharedContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/data/report/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-report-create-1")
                        .content(OBJECT_MAPPER.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.code").value("task-pressure"))
                .andExpect(jsonPath("$.data.metrics[0].metricCode").value("volume"))
                .andExpect(jsonPath("$.meta.requestId").value("req-report-create-1"));

        mockMvc.perform(post("/api/v1/data/report/definitions/task-pressure/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/data/report/definitions/task-pressure/summary")
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-report-summary-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.metrics[0].metricCode").value("volume"))
                .andExpect(jsonPath("$.data.metrics[0].value").value(35.0))
                .andExpect(jsonPath("$.meta.requestId").value("req-report-summary-1"));

        mockMvc.perform(get("/api/v1/data/report/cards/task-pressure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.cardCode").value("task-pressure"))
                .andExpect(jsonPath("$.data.summaryMetric.metricCode").value("volume"))
                .andExpect(jsonPath("$.data.trend.length()").value(2))
                .andExpect(jsonPath("$.data.ranking[0].dimensionValue").value("org-a"));

        mockMvc.perform(get("/api/v1/data/report/definitions/task-pressure/export")
                        .param("organizationCode", "org-a"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"task-pressure-report.csv\""
                ))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("occurredAt")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("org-a")));
    }

    private MockMvc buildMockMvc() {
        ReportTestSupport.InMemoryReportDefinitionRepository definitionRepository =
                new ReportTestSupport.InMemoryReportDefinitionRepository();
        ReportTestSupport.InMemoryReportSnapshotRepository snapshotRepository =
                new ReportTestSupport.InMemoryReportSnapshotRepository();
        InMemoryReportSnapshotCache cache = new InMemoryReportSnapshotCache();
        ReportDefinitionApplicationService definitionApplicationService = new ReportDefinitionApplicationService(
                definitionRepository,
                ReportTestSupport.fixedClock()
        );
        ReportRefreshApplicationService refreshApplicationService = new ReportRefreshApplicationService(
                definitionRepository,
                snapshotRepository,
                cache,
                new DefaultReportDataSourceRegistry(java.util.List.of(ReportTestSupport.sampleProvider())),
                event -> {
                },
                ReportTestSupport.fixedClock()
        );
        ReportQueryApplicationService reportQueryApplicationService = new ReportQueryApplicationService(
                definitionRepository,
                snapshotRepository,
                cache,
                new ReportAnalysisEngine(),
                refreshApplicationService
        );
        ReportEventDrivenRefreshApplicationService eventDrivenRefreshApplicationService =
                new ReportEventDrivenRefreshApplicationService(definitionRepository, refreshApplicationService);
        ReportController controller = new ReportController(
                definitionApplicationService,
                refreshApplicationService,
                reportQueryApplicationService,
                new ResponseMetaFactory()
        );
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new SharedGlobalExceptionHandler(new ResponseMetaFactory()))
                .build();
    }

    private SaveReportDefinitionRequest sampleRequest() {
        return new SaveReportDefinitionRequest(
                "task-pressure",
                "任务压力统计",
                com.hjo2oa.data.report.domain.ReportType.CARD,
                com.hjo2oa.data.report.domain.ReportSourceScope.TASK,
                com.hjo2oa.data.report.domain.ReportRefreshMode.ON_DEMAND,
                com.hjo2oa.data.report.domain.ReportVisibilityMode.PORTAL_CARD,
                ReportTestSupport.TENANT_ID,
                new SaveReportDefinitionRequest.ReportCaliberRequest(
                        ReportTestSupport.PROVIDER_KEY,
                        "TASK",
                        "occurredAt",
                        "organizationCode",
                        "taskDataService",
                        java.util.Map.of("scene", "office-center"),
                        java.util.List.of("process.*"),
                        "任务压力平台统计口径"
                ),
                new SaveReportDefinitionRequest.ReportRefreshConfigRequest(300, 900, 1000),
                new SaveReportDefinitionRequest.ReportCardProtocolRequest(
                        "task-pressure",
                        "待办压力",
                        com.hjo2oa.data.report.domain.ReportCardType.MIXED,
                        "volume",
                        "volume",
                        "volume",
                        "organization",
                        5
                ),
                java.util.List.of(
                        new SaveReportDefinitionRequest.ReportMetricRequest(
                                null,
                                "volume",
                                "任务总量",
                                com.hjo2oa.data.report.domain.ReportMetricAggregationType.SUM,
                                "totalCount",
                                null,
                                null,
                                "件",
                                true,
                                true,
                                0
                        ),
                        new SaveReportDefinitionRequest.ReportMetricRequest(
                                null,
                                "completionRate",
                                "完成率",
                                com.hjo2oa.data.report.domain.ReportMetricAggregationType.RATIO,
                                null,
                                "sum(completedCount)/sum(totalCount)",
                                null,
                                "%",
                                true,
                                false,
                                1
                        )
                ),
                java.util.List.of(
                        new SaveReportDefinitionRequest.ReportDimensionRequest(
                                null,
                                "day",
                                "统计日",
                                com.hjo2oa.data.report.domain.ReportDimensionType.TIME,
                                "occurredAt",
                                com.hjo2oa.data.report.domain.ReportTimeGranularity.DAY,
                                true,
                                0
                        ),
                        new SaveReportDefinitionRequest.ReportDimensionRequest(
                                null,
                                "organization",
                                "组织",
                                com.hjo2oa.data.report.domain.ReportDimensionType.ORGANIZATION,
                                "organizationCode",
                                com.hjo2oa.data.report.domain.ReportTimeGranularity.NONE,
                                true,
                                1
                        )
                )
        );
    }
}
