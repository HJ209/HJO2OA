package com.hjo2oa.wf.action.engine.interfaces;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import com.hjo2oa.wf.action.engine.application.ActionEngineApplicationService;
import com.hjo2oa.wf.action.engine.domain.TaskInstanceSnapshot;
import com.hjo2oa.wf.action.engine.domain.TaskStatus;
import com.hjo2oa.wf.action.engine.infrastructure.InMemoryActionDefinitionRepository;
import com.hjo2oa.wf.action.engine.infrastructure.InMemoryTaskActionRepository;
import com.hjo2oa.wf.action.engine.infrastructure.InMemoryTaskInstanceGateway;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ActionEngineControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldUseSharedWebContractForAvailableActionsAndExecution() throws Exception {
        UUID taskId = UUID.randomUUID();
        MockMvc mockMvc = mockMvc(taskId);

        mockMvc.perform(get("/api/v1/process/tasks/{taskId}/actions", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data", hasSize(5)));

        Map<String, Object> body = Map.of(
                "actionCode",
                "approve",
                "operatorAccountId",
                "operator-1"
        );

        mockMvc.perform(post("/api/v1/process/tasks/{taskId}/actions/execute", taskId)
                        .header("X-Idempotency-Key", "idem-controller-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.action.actionCode").value("approve"))
                .andExpect(jsonPath("$.data.taskStatus").value("COMPLETED"));
    }

    @Test
    void shouldRequireIdempotencyKey() throws Exception {
        UUID taskId = UUID.randomUUID();
        MockMvc mockMvc = mockMvc(taskId);

        Map<String, Object> body = Map.of(
                "actionCode",
                "approve",
                "operatorAccountId",
                "operator-1"
        );

        mockMvc.perform(post("/api/v1/process/tasks/{taskId}/actions/execute", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private MockMvc mockMvc(UUID taskId) {
        InMemoryTaskInstanceGateway taskGateway = new InMemoryTaskInstanceGateway();
        taskGateway.save(new TaskInstanceSnapshot(taskId, UUID.randomUUID(), "user-1", TaskStatus.PENDING, "tenant-1"));
        ActionEngineApplicationService service = new ActionEngineApplicationService(
                taskGateway,
                new InMemoryActionDefinitionRepository(),
                new InMemoryTaskActionRepository(),
                event -> {
                }
        );
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        ActionEngineController controller =
                new ActionEngineController(service, new ActionEngineDtoMapper(), responseMetaFactory);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }
}
