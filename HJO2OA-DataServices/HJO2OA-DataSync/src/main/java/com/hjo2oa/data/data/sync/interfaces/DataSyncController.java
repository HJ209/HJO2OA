package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.application.SyncExecutionApplicationService;
import com.hjo2oa.data.data.sync.application.SyncExecutionDetailView;
import com.hjo2oa.data.data.sync.application.SyncExecutionSummaryView;
import com.hjo2oa.data.data.sync.application.SyncTaskApplicationService;
import com.hjo2oa.data.data.sync.application.SyncTaskDetailView;
import com.hjo2oa.data.data.sync.application.SyncTaskSummaryView;
import com.hjo2oa.data.data.sync.domain.ExecutionStatus;
import com.hjo2oa.data.data.sync.domain.ExecutionTriggerType;
import com.hjo2oa.data.data.sync.domain.PagedResult;
import com.hjo2oa.data.data.sync.domain.SyncExecutionFilter;
import com.hjo2oa.data.data.sync.domain.SyncMode;
import com.hjo2oa.data.data.sync.domain.SyncTaskFilter;
import com.hjo2oa.data.data.sync.domain.SyncTaskStatus;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.Pagination;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data/sync")
@UseSharedWebContract
public class DataSyncController {

    private final SyncTaskApplicationService taskApplicationService;
    private final SyncExecutionApplicationService executionApplicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public DataSyncController(
            SyncTaskApplicationService taskApplicationService,
            SyncExecutionApplicationService executionApplicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.taskApplicationService = taskApplicationService;
        this.executionApplicationService = executionApplicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/tasks")
    public ApiResponse<com.hjo2oa.shared.web.PageData<SyncTaskSummaryView>> pageTasks(
            @RequestParam(name = "tenantId", required = false) UUID tenantId,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "syncMode", required = false) SyncMode syncMode,
            @RequestParam(name = "status", required = false) SyncTaskStatus status,
            @RequestParam(name = "sourceConnectorId", required = false) UUID sourceConnectorId,
            @RequestParam(name = "targetConnectorId", required = false) UUID targetConnectorId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        PagedResult<SyncTaskSummaryView> result = taskApplicationService.pageTasks(new SyncTaskFilter(
                tenantId,
                code,
                syncMode,
                status,
                sourceConnectorId,
                targetConnectorId,
                page,
                size
        ));
        return ApiResponse.page(
                result.items(),
                Pagination.of(result.page(), result.size(), result.total()),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<SyncTaskDetailView> getTask(@PathVariable("taskId") UUID taskId, HttpServletRequest request) {
        return ApiResponse.success(taskApplicationService.getTask(taskId), responseMetaFactory.create(request));
    }

    @PostMapping("/tasks")
    public ApiResponse<SyncTaskDetailView> createTask(
            @Valid @RequestBody CreateSyncExchangeTaskRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(taskApplicationService.create(body.toCommand()), responseMetaFactory.create(request));
    }

    @PutMapping("/tasks/{taskId}")
    public ApiResponse<SyncTaskDetailView> updateTask(
            @PathVariable("taskId") UUID taskId,
            @Valid @RequestBody UpdateSyncExchangeTaskRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(taskApplicationService.update(taskId, body.toCommand()), responseMetaFactory.create(request));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable("taskId") UUID taskId, HttpServletRequest request) {
        taskApplicationService.delete(taskId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @PostMapping("/tasks/{taskId}/activate")
    public ApiResponse<SyncTaskDetailView> activateTask(
            @PathVariable("taskId") UUID taskId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(taskApplicationService.activate(taskId), responseMetaFactory.create(request));
    }

    @PostMapping("/tasks/{taskId}/pause")
    public ApiResponse<SyncTaskDetailView> pauseTask(
            @PathVariable("taskId") UUID taskId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(taskApplicationService.pause(taskId), responseMetaFactory.create(request));
    }

    @PostMapping("/tasks/{taskId}/checkpoint/reset")
    public ApiResponse<SyncTaskDetailView> resetCheckpoint(
            @PathVariable("taskId") UUID taskId,
            @Valid @RequestBody ResetSyncCheckpointRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(taskApplicationService.resetCheckpoint(taskId, body.toCommand()), responseMetaFactory.create(request));
    }

    @PostMapping("/tasks/{taskId}/trigger")
    public ApiResponse<SyncExecutionDetailView> triggerTask(
            @PathVariable("taskId") UUID taskId,
            @Valid @RequestBody TriggerSyncTaskRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(executionApplicationService.triggerTask(taskId, body.toCommand()), responseMetaFactory.create(request));
    }

    @GetMapping("/tasks/{taskId}/executions")
    public ApiResponse<com.hjo2oa.shared.web.PageData<SyncExecutionSummaryView>> pageTaskExecutions(
            @PathVariable("taskId") UUID taskId,
            @RequestParam(name = "executionStatus", required = false) ExecutionStatus executionStatus,
            @RequestParam(name = "triggerType", required = false) ExecutionTriggerType triggerType,
            @RequestParam(name = "startedFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
            @RequestParam(name = "startedTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        PagedResult<SyncExecutionSummaryView> result = taskApplicationService.pageExecutions(new SyncExecutionFilter(
                taskId,
                null,
                executionStatus,
                triggerType,
                startedFrom,
                startedTo,
                page,
                size
        ));
        return ApiResponse.page(
                result.items(),
                Pagination.of(result.page(), result.size(), result.total()),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/executions")
    public ApiResponse<com.hjo2oa.shared.web.PageData<SyncExecutionSummaryView>> pageExecutions(
            @RequestParam(name = "taskCode", required = false) String taskCode,
            @RequestParam(name = "executionStatus", required = false) ExecutionStatus executionStatus,
            @RequestParam(name = "triggerType", required = false) ExecutionTriggerType triggerType,
            @RequestParam(name = "startedFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
            @RequestParam(name = "startedTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        PagedResult<SyncExecutionSummaryView> result = taskApplicationService.pageExecutions(new SyncExecutionFilter(
                null,
                taskCode,
                executionStatus,
                triggerType,
                startedFrom,
                startedTo,
                page,
                size
        ));
        return ApiResponse.page(
                result.items(),
                Pagination.of(result.page(), result.size(), result.total()),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/executions/{executionId}")
    public ApiResponse<SyncExecutionDetailView> getExecution(
            @PathVariable("executionId") UUID executionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(taskApplicationService.getExecution(executionId), responseMetaFactory.create(request));
    }

    @PostMapping("/executions/{executionId}/retry")
    public ApiResponse<SyncExecutionDetailView> retryExecution(
            @PathVariable("executionId") UUID executionId,
            @Valid @RequestBody RetrySyncExecutionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(executionApplicationService.retryExecution(executionId, body.toCommand()), responseMetaFactory.create(request));
    }

    @PostMapping("/executions/{executionId}/reconcile")
    public ApiResponse<SyncExecutionDetailView> reconcileExecution(
            @PathVariable("executionId") UUID executionId,
            @Valid @RequestBody ReconcileSyncExecutionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(executionApplicationService.reconcileExecution(executionId, body.toCommand()), responseMetaFactory.create(request));
    }

    @PostMapping("/executions/{executionId}/compensate")
    public ApiResponse<SyncExecutionDetailView> compensateExecution(
            @PathVariable("executionId") UUID executionId,
            @Valid @RequestBody SubmitManualCompensationRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(executionApplicationService.compensateExecution(executionId, body.toCommand()), responseMetaFactory.create(request));
    }
}
