package com.hjo2oa.todo.center.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.todo.center.application.TodoQueryApplicationService;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemRepository;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoItemRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TodoCenterControllerTest {

    @Test
    void shouldReturnCompletedTodos() throws Exception {
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        repository.save(new TodoItem(
                "todo-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Completed request",
                "LOW",
                TodoItemStatus.COMPLETED,
                null,
                null,
                Instant.parse("2026-04-19T09:00:00Z"),
                Instant.parse("2026-04-19T12:00:00Z"),
                Instant.parse("2026-04-19T12:00:00Z"),
                null
        ));
        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        TodoQueryApplicationService service = new TodoQueryApplicationService(repository, identityContextProvider);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TodoCenterController(service)).build();

        mockMvc.perform(get("/api/v1/todo/completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].todoId").value("todo-1"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void shouldReturnOverdueTodos() throws Exception {
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        repository.save(new TodoItem(
                "todo-2",
                "task-2",
                "instance-2",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Overdue request",
                "HIGH",
                TodoItemStatus.PENDING,
                Instant.parse("2026-04-19T08:00:00Z"),
                Instant.parse("2026-04-19T10:00:00Z"),
                Instant.parse("2026-04-19T07:00:00Z"),
                Instant.parse("2026-04-19T10:00:00Z"),
                null,
                null
        ));
        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        TodoQueryApplicationService service = new TodoQueryApplicationService(repository, identityContextProvider);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TodoCenterController(service)).build();

        mockMvc.perform(get("/api/v1/todo/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].todoId").value("todo-2"))
                .andExpect(jsonPath("$[0].overdueAt").exists());
    }
}
