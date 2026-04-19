package com.hjo2oa.todo.center.interfaces;

import com.hjo2oa.todo.center.application.TodoQueryApplicationService;
import com.hjo2oa.todo.center.domain.TodoCounts;
import com.hjo2oa.todo.center.domain.TodoItemSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/todo")
public class TodoCenterController {

    private final TodoQueryApplicationService todoQueryApplicationService;

    public TodoCenterController(TodoQueryApplicationService todoQueryApplicationService) {
        this.todoQueryApplicationService = todoQueryApplicationService;
    }

    @GetMapping("/pending")
    public List<TodoItemSummary> pending() {
        return todoQueryApplicationService.pendingTodos();
    }

    @GetMapping("/completed")
    public List<TodoItemSummary> completed() {
        return todoQueryApplicationService.completedTodos();
    }

    @GetMapping("/overdue")
    public List<TodoItemSummary> overdue() {
        return todoQueryApplicationService.overdueTodos();
    }

    @GetMapping("/counts")
    public TodoCounts counts() {
        return todoQueryApplicationService.counts();
    }
}
