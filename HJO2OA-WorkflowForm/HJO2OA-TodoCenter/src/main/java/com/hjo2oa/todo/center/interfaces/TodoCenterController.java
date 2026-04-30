package com.hjo2oa.todo.center.interfaces;

import com.hjo2oa.todo.center.application.CopiedTodoActionApplicationService;
import com.hjo2oa.todo.center.application.TodoActionApplicationService;
import com.hjo2oa.todo.center.application.TodoQueryApplicationService;
import com.hjo2oa.todo.center.domain.ArchiveProcessSummary;
import com.hjo2oa.todo.center.domain.CopiedTodoReadStatus;
import com.hjo2oa.todo.center.domain.CopiedTodoSummary;
import com.hjo2oa.todo.center.domain.DraftProcessSummary;
import com.hjo2oa.todo.center.domain.InitiatedProcessSummary;
import com.hjo2oa.todo.center.domain.TodoBatchActionResult;
import com.hjo2oa.todo.center.domain.TodoCounts;
import com.hjo2oa.todo.center.domain.TodoItemSummary;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/todo")
public class TodoCenterController {

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final TodoQueryApplicationService todoQueryApplicationService;
    private final TodoActionApplicationService todoActionApplicationService;
    private final CopiedTodoActionApplicationService copiedTodoActionApplicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public TodoCenterController(
            TodoQueryApplicationService todoQueryApplicationService,
            TodoActionApplicationService todoActionApplicationService,
            CopiedTodoActionApplicationService copiedTodoActionApplicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.todoQueryApplicationService = todoQueryApplicationService;
        this.todoActionApplicationService = todoActionApplicationService;
        this.copiedTodoActionApplicationService = copiedTodoActionApplicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/pending")
    public ApiResponse<List<TodoItemSummary>> pending(HttpServletRequest request) {
        return ApiResponse.success(todoQueryApplicationService.pendingTodos(), responseMetaFactory.create(request));
    }

    @GetMapping("/completed")
    public ApiResponse<List<TodoItemSummary>> completed(HttpServletRequest request) {
        return ApiResponse.success(todoQueryApplicationService.completedTodos(), responseMetaFactory.create(request));
    }

    @GetMapping("/overdue")
    public ApiResponse<List<TodoItemSummary>> overdue(HttpServletRequest request) {
        return ApiResponse.success(todoQueryApplicationService.overdueTodos(), responseMetaFactory.create(request));
    }

    @GetMapping("/copied")
    public ApiResponse<List<CopiedTodoSummary>> copied(
            @RequestParam(name = "filter[readStatus]", required = false) CopiedTodoReadStatus readStatus,
            HttpServletRequest request
    ) {
        return ApiResponse.success(todoQueryApplicationService.copiedTodos(readStatus), responseMetaFactory.create(request));
    }

    @GetMapping("/initiated")
    public ApiResponse<List<InitiatedProcessSummary>> initiated(HttpServletRequest request) {
        return ApiResponse.success(todoQueryApplicationService.initiatedProcesses(), responseMetaFactory.create(request));
    }

    @GetMapping("/drafts")
    public ApiResponse<List<DraftProcessSummary>> drafts(HttpServletRequest request) {
        return ApiResponse.success(todoQueryApplicationService.draftProcesses(), responseMetaFactory.create(request));
    }

    @GetMapping("/archives")
    public ApiResponse<List<ArchiveProcessSummary>> archives(HttpServletRequest request) {
        return ApiResponse.success(todoQueryApplicationService.archivedProcesses(), responseMetaFactory.create(request));
    }

    @GetMapping("/{todoId}")
    public ApiResponse<TodoItemSummary> detail(@PathVariable String todoId, HttpServletRequest request) {
        return ApiResponse.success(todoActionApplicationService.detail(todoId), responseMetaFactory.create(request));
    }

    @PostMapping("/{todoId}/complete")
    public ApiResponse<TodoItemSummary> complete(
            @PathVariable String todoId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                todoActionApplicationService.complete(todoId, requireIdempotencyKey(idempotencyKey)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/batch/complete")
    public ApiResponse<TodoBatchActionResult> batchComplete(
            @RequestBody BatchTodoActionRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                todoActionApplicationService.batchComplete(body.todoIds(), requireIdempotencyKey(idempotencyKey)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{todoId}/remind")
    public ApiResponse<TodoItemSummary> remind(
            @PathVariable String todoId,
            @RequestBody(required = false) RemindTodoRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                todoActionApplicationService.remind(
                        todoId,
                        body == null ? null : body.reason(),
                        requireIdempotencyKey(idempotencyKey)
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/batch/remind")
    public ApiResponse<TodoBatchActionResult> batchRemind(
            @RequestBody BatchTodoActionRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                todoActionApplicationService.batchRemind(
                        body.todoIds(),
                        body.reason(),
                        requireIdempotencyKey(idempotencyKey)
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/copied/{todoId}/read")
    public ApiResponse<CopiedTodoSummary> markCopiedAsRead(
            @PathVariable String todoId,
            HttpServletRequest request
    ) {
        CopiedTodoSummary copiedTodo = copiedTodoActionApplicationService.markRead(todoId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Copied todo not found"));
        return ApiResponse.success(copiedTodo, responseMetaFactory.create(request));
    }

    @PostMapping("/copied/batch-read")
    public ApiResponse<TodoBatchActionResult> batchReadCopied(
            @RequestBody BatchTodoActionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                copiedTodoActionApplicationService.batchMarkRead(body.todoIds()),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/counts")
    public ApiResponse<TodoCounts> counts(HttpServletRequest request) {
        return ApiResponse.success(todoQueryApplicationService.counts(), responseMetaFactory.create(request));
    }

    private String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "X-Idempotency-Key is required");
        }
        return idempotencyKey.trim();
    }

    public record BatchTodoActionRequest(List<String> todoIds, String reason) {
    }

    public record RemindTodoRequest(String reason) {
    }
}
