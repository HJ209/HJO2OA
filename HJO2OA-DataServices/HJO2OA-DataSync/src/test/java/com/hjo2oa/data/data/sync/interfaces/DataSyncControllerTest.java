package com.hjo2oa.data.data.sync.interfaces;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.common.interfaces.web.DataServicesGlobalExceptionHandler;
import com.hjo2oa.data.common.domain.exception.DataServicesErrorCode;
import com.hjo2oa.data.data.sync.DataSyncTestSupport;
import com.hjo2oa.data.data.sync.application.SyncExecutionApplicationService;
import com.hjo2oa.data.data.sync.application.SyncTaskApplicationService;
import com.hjo2oa.data.data.sync.domain.CheckpointMode;
import com.hjo2oa.data.data.sync.domain.ConflictStrategy;
import com.hjo2oa.data.data.sync.domain.SyncCheckpointConfig;
import com.hjo2oa.data.data.sync.domain.SyncCompensationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncMode;
import com.hjo2oa.data.data.sync.domain.SyncReconciliationPolicy;
import com.hjo2oa.data.data.sync.domain.SyncRetryPolicy;
import com.hjo2oa.data.data.sync.domain.SyncScheduleConfig;
import com.hjo2oa.data.data.sync.domain.SyncTriggerConfig;
import com.hjo2oa.data.data.sync.infrastructure.InMemorySyncConnectorGateway;
import com.hjo2oa.data.data.sync.infrastructure.InMemorySyncExchangeTaskRepository;
import com.hjo2oa.data.data.sync.infrastructure.InMemorySyncExecutionRecordRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class DataSyncControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T04:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private InMemorySyncConnectorGateway connectorGateway;
    private SyncTaskApplicationService taskApplicationService;
    private SyncExecutionApplicationService executionApplicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InMemorySyncExchangeTaskRepository taskRepository = new InMemorySyncExchangeTaskRepository();
        InMemorySyncExecutionRecordRepository executionRecordRepository = new InMemorySyncExecutionRecordRepository();
        connectorGateway = new InMemorySyncConnectorGateway();
        connectorGateway.registerConnector(DataSyncTestSupport.SOURCE_CONNECTOR_ID, "ACTIVE");
        connectorGateway.registerConnector(DataSyncTestSupport.TARGET_CONNECTOR_ID, "ACTIVE");
        DataSyncTestSupport.CollectingDataDomainEventPublisher eventPublisher =
                new DataSyncTestSupport.CollectingDataDomainEventPublisher();
        Clock clock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
        taskApplicationService = new SyncTaskApplicationService(
                taskRepository,
                executionRecordRepository,
                connectorGateway,
                clock
        );
        executionApplicationService = new SyncExecutionApplicationService(
                taskRepository,
                executionRecordRepository,
                connectorGateway,
                eventPublisher,
                clock
        );
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DataSyncController(taskApplicationService, executionApplicationService, responseMetaFactory))
                .setControllerAdvice(new DataServicesGlobalExceptionHandler(responseMetaFactory))
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void shouldCreateTaskUsingSharedWebContract() throws Exception {
        mockMvc.perform(post("/api/v1/data/sync/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-sync-create-1")
                        .content("""
                                {
                                  "tenantId":"11111111-1111-1111-1111-111111111111",
                                  "code":"sync-controller-create",
                                  "name":"sync-controller-create",
                                  "description":"controller create",
                                  "taskType":"EXPORT",
                                  "syncMode":"FULL",
                                  "sourceConnectorId":"22222222-2222-2222-2222-222222222222",
                                  "targetConnectorId":"33333333-3333-3333-3333-333333333333",
                                  "checkpointMode":"NONE",
                                  "triggerConfig":{"manualTriggerEnabled":true,"eventPatterns":[]},
                                  "retryPolicy":{"maxRetries":3,"manualRetryEnabled":true,"automaticRetryEnabled":false,"retryableErrorCodes":[]},
                                  "compensationPolicy":{"manualCompensationEnabled":true,"allowIgnoreDifference":true,"requireReason":true,"maxCompensationAttempts":3},
                                  "reconciliationPolicy":{"enabled":false,"checkExtraTargetRecords":false,"failWhenDifferenceDetected":false,"manualReviewThreshold":0},
                                  "scheduleConfig":{"enabled":false},
                                  "mappingRules":[
                                    {"sourceField":"id","targetField":"id","conflictStrategy":"OVERWRITE","keyMapping":true,"sortOrder":0},
                                    {"sourceField":"name","targetField":"name","transformRule":{"operation":"TRIM"},"conflictStrategy":"OVERWRITE","keyMapping":false,"sortOrder":1}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.summary.code").value("sync-controller-create"))
                .andExpect(jsonPath("$.data.summary.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.mappingRules[*].targetField", hasItem("name")))
                .andExpect(jsonPath("$.meta.requestId").value("req-sync-create-1"));
    }

    @Test
    void shouldTriggerTaskAndQueryExecutionsUsingSharedWebContract() throws Exception {
        var task = taskApplicationService.create(DataSyncTestSupport.createTaskCommand(
                "sync-controller-execution",
                SyncMode.FULL,
                CheckpointMode.NONE,
                SyncCheckpointConfig.empty(),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(false, false, false, 0),
                SyncScheduleConfig.disabled(),
                ConflictStrategy.OVERWRITE
        ));
        taskApplicationService.activate(task.summary().taskId());
        connectorGateway.upsertRecord(
                DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord(
                        "controller-user-1",
                        "1",
                        null,
                        FIXED_TIME,
                        Map.of("id", "controller-user-1", "name", " Controller User ")
                )
        );

        MvcResult triggerResult = mockMvc.perform(post("/api/v1/data/sync/tasks/{taskId}/trigger", task.summary().taskId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-sync-trigger-1")
                        .content("""
                                {
                                  "idempotencyKey":"controller-batch-1",
                                  "operatorAccountId":"ops-admin",
                                  "operatorPersonId":"person-1",
                                  "triggerContext":{"source":"controller"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.summary.executionStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.summary.taskCode").value("sync-controller-execution"))
                .andExpect(jsonPath("$.meta.requestId").value("req-sync-trigger-1"))
                .andReturn();

        JsonNode triggerBody = objectMapper.readTree(triggerResult.getResponse().getContentAsString());
        String executionId = triggerBody.path("data").path("summary").path("executionId").asText();

        mockMvc.perform(get("/api/v1/data/sync/tasks/{taskId}/executions", task.summary().taskId())
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].executionStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].taskCode").value("sync-controller-execution"));

        mockMvc.perform(get("/api/v1/data/sync/executions/{executionId}", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.executionId").value(executionId))
                .andExpect(jsonPath("$.data.operatorAccountId").value("ops-admin"));
    }

    @Test
    void shouldRejectInvalidTaskRequestWithValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/data/sync/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"11111111-1111-1111-1111-111111111111",
                                  "taskType":"EXPORT",
                                  "syncMode":"FULL"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR.code()))
                .andExpect(jsonPath("$.errors[*].field", hasItem("code")));
    }

    @Test
    void shouldCompensateFailedExecutionThroughController() throws Exception {
        var task = taskApplicationService.create(DataSyncTestSupport.createTaskCommand(
                "sync-controller-compensate",
                SyncMode.FULL,
                CheckpointMode.NONE,
                SyncCheckpointConfig.empty(),
                SyncTriggerConfig.manualOnly(),
                SyncRetryPolicy.manualOnly(),
                SyncCompensationPolicy.manualDefault(),
                new SyncReconciliationPolicy(true, true, true, 0),
                SyncScheduleConfig.disabled(),
                ConflictStrategy.OVERWRITE
        ));
        taskApplicationService.activate(task.summary().taskId());
        connectorGateway.upsertRecord(
                DataSyncTestSupport.SOURCE_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord(
                        "controller-user-2",
                        "2",
                        null,
                        FIXED_TIME,
                        Map.of("id", "controller-user-2", "name", "Controller User 2")
                )
        );
        connectorGateway.upsertRecord(
                DataSyncTestSupport.TARGET_CONNECTOR_ID,
                DataSyncTestSupport.payloadRecord(
                        "stale-controller-user",
                        "3",
                        null,
                        FIXED_TIME,
                        Map.of("id", "stale-controller-user", "name", "Stale Controller User")
                )
        );
        var failed = executionApplicationService.triggerTask(
                task.summary().taskId(),
                new com.hjo2oa.data.data.sync.application.TriggerSyncTaskCommand(
                        "controller-compensate-batch",
                        "ops-admin",
                        "person-1",
                        Map.of()
                )
        );

        mockMvc.perform(post("/api/v1/data/sync/executions/{executionId}/compensate", failed.summary().executionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey":"controller-compensation-1",
                                  "operatorAccountId":"ops-admin",
                                  "operatorPersonId":"person-1",
                                  "reason":"remove stale record",
                                  "decisions":[
                                    {
                                      "differenceCode":"%s",
                                      "action":"RETRY_WRITE",
                                      "reason":"delete stale target"
                                    }
                                  ]
                                }
                                """.formatted(failed.differences().get(0).differenceCode())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.summary.executionStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.differences[0].status").value("COMPENSATED"));
    }
}
