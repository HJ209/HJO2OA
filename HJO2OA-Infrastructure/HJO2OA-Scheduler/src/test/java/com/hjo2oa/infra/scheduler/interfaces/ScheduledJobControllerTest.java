package com.hjo2oa.infra.scheduler.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.scheduler.application.ScheduledJobApplicationService;
import com.hjo2oa.infra.scheduler.application.SchedulerExecutionService;
import com.hjo2oa.infra.scheduler.application.SchedulerJobExecutionContext;
import com.hjo2oa.infra.scheduler.application.SchedulerJobHandler;
import com.hjo2oa.infra.scheduler.application.SchedulerJobHandlerRegistry;
import com.hjo2oa.infra.scheduler.application.SchedulerJobResult;
import com.hjo2oa.infra.scheduler.infrastructure.InMemoryJobExecutionRecordRepository;
import com.hjo2oa.infra.scheduler.infrastructure.InMemoryScheduledJobRepository;
import com.hjo2oa.infra.scheduler.infrastructure.InMemorySchedulerJobLock;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ScheduledJobControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T00:00:00Z");

    @Test
    void shouldRegisterTriggerAndQueryJobUsingSharedContract() throws Exception {
        try (Fixture fixture = new Fixture()) {
            ScheduledJobDtoMapper dtoMapper = new ScheduledJobDtoMapper();
            ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
            LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
            validator.afterPropertiesSet();
            MockMvc mockMvc = MockMvcBuilders
                    .standaloneSetup(
                            new ScheduledJobController(fixture.applicationService, dtoMapper, responseMetaFactory),
                            new SchedulerExecutionController(fixture.applicationService, dtoMapper, responseMetaFactory)
                    )
                    .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                    .setValidator(validator)
                    .setMessageConverters(new MappingJackson2HttpMessageConverter())
                    .build();

            mockMvc.perform(post("/api/v1/infra/scheduler/jobs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "jobCode":"manual-reconcile",
                                      "handlerName":"success-handler",
                                      "name":"Manual Reconcile",
                                      "triggerType":"MANUAL",
                                      "concurrencyPolicy":"FORBID",
                                      "timeoutSeconds":180
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.jobCode").value("manual-reconcile"))
                    .andExpect(jsonPath("$.data.handlerName").value("success-handler"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.meta.requestId").exists());

            String jobId = fixture.applicationService.queryJobs(null).get(0).id().toString();

            mockMvc.perform(post("/api/v1/infra/scheduler/jobs/{jobId}/trigger", jobId)
                            .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-trigger-1")
                            .header("X-Idempotency-Key", "idem-trigger-1")
                            .header("Accept-Language", "zh-CN")
                            .header("X-Timezone", "Asia/Shanghai"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.executionStatus").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.durationMs").value(0))
                    .andExpect(jsonPath("$.data.triggerSource").value("MANUAL"))
                    .andExpect(jsonPath("$.data.triggerContext").exists());

            mockMvc.perform(get("/api/v1/infra/scheduler/jobs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].jobCode").value("manual-reconcile"));

            mockMvc.perform(get("/api/v1/infra/scheduler/executions")
                            .param("jobId", jobId)
                            .param("executionStatus", "SUCCESS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].executionStatus").value("SUCCESS"));

            String executionId = fixture.applicationService.queryExecutions(
                    fixture.applicationService.queryJobs(null).get(0).id(),
                    null,
                    null
            ).get(0).id().toString();
            mockMvc.perform(get("/api/v1/infra/scheduler/executions/{executionId}", executionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(executionId));
        }
    }

    private static final class Fixture implements AutoCloseable {

        private final InMemoryScheduledJobRepository scheduledJobRepository = new InMemoryScheduledJobRepository();
        private final InMemoryJobExecutionRecordRepository executionRecordRepository =
                new InMemoryJobExecutionRecordRepository();
        private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        private final ExecutorService handlerExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        private final ScheduledJobApplicationService applicationService;

        private Fixture() {
            taskScheduler.initialize();
            SchedulerExecutionService executionService = new SchedulerExecutionService(
                    scheduledJobRepository,
                    executionRecordRepository,
                    new SchedulerJobHandlerRegistry(List.of(new SuccessHandler())),
                    new InMemorySchedulerJobLock(),
                    event -> {
                    },
                    taskScheduler,
                    handlerExecutor,
                    Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
            );
            applicationService = new ScheduledJobApplicationService(
                    scheduledJobRepository,
                    executionRecordRepository,
                    event -> {
                    },
                    executionService,
                    Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
            );
        }

        @Override
        public void close() {
            taskScheduler.shutdown();
            handlerExecutor.shutdownNow();
        }
    }

    private static final class SuccessHandler implements SchedulerJobHandler {

        @Override
        public String handlerName() {
            return "success-handler";
        }

        @Override
        public SchedulerJobResult execute(SchedulerJobExecutionContext context) {
            return SchedulerJobResult.success("{\"result\":\"ok\"}");
        }
    }
}
