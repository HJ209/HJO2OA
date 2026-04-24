package com.hjo2oa.todo.center.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.todo.center.application.CopiedTodoActionApplicationService;
import com.hjo2oa.todo.center.application.TodoQueryApplicationService;
import com.hjo2oa.todo.center.domain.CopiedTodoItem;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemRepository;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.infrastructure.InMemoryCopiedTodoRepository;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoItemRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
        InMemoryCopiedTodoRepository copiedTodoRepository = new InMemoryCopiedTodoRepository();
        TodoQueryApplicationService service = new TodoQueryApplicationService(
                repository,
                copiedTodoRepository,
                identityContextProvider
        );
        CopiedTodoActionApplicationService copiedTodoActionApplicationService =
                new CopiedTodoActionApplicationService(copiedTodoRepository, identityContextProvider);
        MockMvc mockMvc = mockMvc(service, copiedTodoActionApplicationService);

        mockMvc.perform(get("/api/v1/todo/completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].todoId").value("todo-1"))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
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
        InMemoryCopiedTodoRepository copiedTodoRepository = new InMemoryCopiedTodoRepository();
        TodoQueryApplicationService service = new TodoQueryApplicationService(
                repository,
                copiedTodoRepository,
                identityContextProvider
        );
        CopiedTodoActionApplicationService copiedTodoActionApplicationService =
                new CopiedTodoActionApplicationService(copiedTodoRepository, identityContextProvider);
        MockMvc mockMvc = mockMvc(service, copiedTodoActionApplicationService);

        mockMvc.perform(get("/api/v1/todo/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].todoId").value("todo-2"))
                .andExpect(jsonPath("$.data[0].overdueAt").exists());
    }

    @Test
    void shouldReturnCopiedTodosFilteredByReadStatus() throws Exception {
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        InMemoryCopiedTodoRepository copiedTodoRepository = new InMemoryCopiedTodoRepository();
        copiedTodoRepository.save(CopiedTodoItem.unread(
                "copied-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Unread copied todo",
                "HIGH",
                Instant.parse("2026-04-19T09:00:00Z")
        ));
        copiedTodoRepository.save(CopiedTodoItem.unread(
                "copied-2",
                "task-2",
                "instance-2",
                "assignment-1",
                "APPROVAL",
                "PROCUREMENT",
                "Read copied todo",
                "NORMAL",
                Instant.parse("2026-04-19T08:00:00Z")
        ).markRead(Instant.parse("2026-04-19T09:30:00Z")));
        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        TodoQueryApplicationService service = new TodoQueryApplicationService(
                repository,
                copiedTodoRepository,
                identityContextProvider
        );
        CopiedTodoActionApplicationService copiedTodoActionApplicationService =
                new CopiedTodoActionApplicationService(copiedTodoRepository, identityContextProvider);
        MockMvc mockMvc = mockMvc(service, copiedTodoActionApplicationService);

        mockMvc.perform(get("/api/v1/todo/copied")
                        .param("filter[readStatus]", "UNREAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].todoId").value("copied-1"))
                .andExpect(jsonPath("$.data[0].readStatus").value("UNREAD"));
    }

    @Test
    void shouldMarkCopiedTodoAsRead() throws Exception {
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        InMemoryCopiedTodoRepository copiedTodoRepository = new InMemoryCopiedTodoRepository();
        copiedTodoRepository.save(CopiedTodoItem.unread(
                "copied-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Unread copied todo",
                "HIGH",
                Instant.parse("2026-04-19T09:00:00Z")
        ));
        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        TodoQueryApplicationService service = new TodoQueryApplicationService(
                repository,
                copiedTodoRepository,
                identityContextProvider
        );
        CopiedTodoActionApplicationService copiedTodoActionApplicationService =
                new CopiedTodoActionApplicationService(
                        copiedTodoRepository,
                        identityContextProvider,
                        Clock.fixed(Instant.parse("2026-04-19T10:00:00Z"), ZoneOffset.UTC)
                );
        MockMvc mockMvc = mockMvc(service, copiedTodoActionApplicationService);

        mockMvc.perform(post("/api/v1/todo/copied/copied-1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.todoId").value("copied-1"))
                .andExpect(jsonPath("$.data.readStatus").value("READ"))
                .andExpect(jsonPath("$.data.readAt").exists());
    }

    @Test
    void shouldReturnNotFoundWhenCopiedTodoIsNotVisible() throws Exception {
        TodoItemRepository repository = new InMemoryTodoItemRepository();
        InMemoryCopiedTodoRepository copiedTodoRepository = new InMemoryCopiedTodoRepository();
        copiedTodoRepository.save(CopiedTodoItem.unread(
                "copied-1",
                "task-1",
                "instance-1",
                "assignment-2",
                "APPROVAL",
                "EXPENSE",
                "Invisible copied todo",
                "HIGH",
                Instant.parse("2026-04-19T09:00:00Z")
        ));
        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        TodoQueryApplicationService service = new TodoQueryApplicationService(
                repository,
                copiedTodoRepository,
                identityContextProvider
        );
        CopiedTodoActionApplicationService copiedTodoActionApplicationService =
                new CopiedTodoActionApplicationService(copiedTodoRepository, identityContextProvider);
        MockMvc mockMvc = mockMvc(service, copiedTodoActionApplicationService);

        mockMvc.perform(post("/api/v1/todo/copied/copied-1/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private static MockMvc mockMvc(
            TodoQueryApplicationService service,
            CopiedTodoActionApplicationService copiedTodoActionApplicationService
    ) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        return MockMvcBuilders.standaloneSetup(
                        new TodoCenterController(service, copiedTodoActionApplicationService, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }
}
