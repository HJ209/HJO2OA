package com.hjo2oa.todo.center.interfaces;

import com.hjo2oa.todo.center.application.CopiedTodoActionApplicationService;
import com.hjo2oa.todo.center.application.TodoQueryApplicationService;
import com.hjo2oa.todo.center.domain.CopiedTodoReadStatus;
import com.hjo2oa.todo.center.domain.CopiedTodoSummary;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/todo")
public class TodoCenterController {

    private final TodoQueryApplicationService todoQueryApplicationService;
    private final CopiedTodoActionApplicationService copiedTodoActionApplicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public TodoCenterController(
            TodoQueryApplicationService todoQueryApplicationService,
            CopiedTodoActionApplicationService copiedTodoActionApplicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.todoQueryApplicationService = todoQueryApplicationService;
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

    @PostMapping("/copied/{todoId}/read")
    public ApiResponse<CopiedTodoSummary> markCopiedAsRead(
            @PathVariable String todoId,
            HttpServletRequest request
    ) {
        CopiedTodoSummary copiedTodo = copiedTodoActionApplicationService.markRead(todoId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Copied todo not found"));
        return ApiResponse.success(copiedTodo, responseMetaFactory.create(request));
    }

    @GetMapping("/counts")
    public ApiResponse<TodoCounts> counts(HttpServletRequest request) {
        return ApiResponse.success(todoQueryApplicationService.counts(), responseMetaFactory.create(request));
    }
}
