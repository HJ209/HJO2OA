package com.hjo2oa.infra.scheduler.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.scheduler.application.ScheduledJobApplicationService;
import com.hjo2oa.infra.scheduler.infrastructure.InMemoryJobExecutionRecordRepository;
import com.hjo2oa.infra.scheduler.infrastructure.InMemoryScheduledJobRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ScheduledJobControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T00:00:00Z");

    @Test
    void shouldRegisterTriggerAndQueryJobUsingSharedContract() throws Exception {
        ScheduledJobApplicationService applicationService = new ScheduledJobApplicationService(
                new InMemoryScheduledJobRepository(),
                new InMemoryJobExecutionRecordRepository(),
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        ScheduledJobDtoMapper dtoMapper = new ScheduledJobDtoMapper();
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ScheduledJobController(applicationService, dtoMapper, responseMetaFactory))
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/v1/infra/scheduler/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobCode":"manual-reconcile",
                                  "name":"Manual Reconcile",
                                  "triggerType":"MANUAL",
                                  "concurrencyPolicy":"FORBID",
                                  "timeoutSeconds":180
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.jobCode").value("manual-reconcile"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.meta.requestId").exists());

        String jobId = applicationService.queryJobs(null).get(0).id().toString();

        mockMvc.perform(post("/api/v1/infra/scheduler/jobs/trigger/manual-reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executionStatus").value("RUNNING"))
                .andExpect(jsonPath("$.data.triggerSource").value("MANUAL"));

        mockMvc.perform(get("/api/v1/infra/scheduler/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].jobCode").value("manual-reconcile"));

        mockMvc.perform(get("/api/v1/infra/scheduler/jobs/{jobId}/executions", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].executionStatus").value("RUNNING"));
    }
}
