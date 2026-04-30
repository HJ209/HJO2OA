package com.hjo2oa.portal.aggregation.api.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.aggregation.api.domain.PortalTodoListView;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoListViewType;
import com.hjo2oa.todo.center.application.TodoQueryApplicationService;
import com.hjo2oa.todo.center.domain.CopiedTodoItem;
import com.hjo2oa.todo.center.domain.CopiedTodoReadStatus;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.infrastructure.InMemoryCopiedTodoRepository;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoItemRepository;
import com.hjo2oa.todo.center.infrastructure.InMemoryTodoProcessViewRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PortalTodoListAggregationApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-19T10:00:00Z");

    @Test
    void shouldReturnPendingTodoPageWithSummaryAndFilters() {
        PortalTodoListAggregationApplicationService service = new PortalTodoListAggregationApplicationService(
                todoQueryApplicationService(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalTodoListView view = service.officeCenterTodos(
                1,
                1,
                PortalTodoListViewType.PENDING,
                "approval",
                true,
                "travel",
                null
        );

        assertThat(view.viewType()).isEqualTo(PortalTodoListViewType.PENDING);
        assertThat(view.summary().pendingCount()).isEqualTo(2);
        assertThat(view.summary().completedCount()).isEqualTo(1);
        assertThat(view.summary().overdueCount()).isEqualTo(1);
        assertThat(view.summary().copiedUnreadCount()).isEqualTo(1);
        assertThat(view.todos().pagination().total()).isEqualTo(1);
        assertThat(view.todos().items())
                .extracting(item -> item.todoId() + ":" + item.status())
                .containsExactly("todo-1:PENDING");
    }

    @Test
    void shouldReturnCopiedTodoPageFilteredByReadStatus() {
        PortalTodoListAggregationApplicationService service = new PortalTodoListAggregationApplicationService(
                todoQueryApplicationService(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalTodoListView view = service.officeCenterTodos(
                1,
                20,
                PortalTodoListViewType.COPIED,
                null,
                false,
                null,
                CopiedTodoReadStatus.UNREAD
        );

        assertThat(view.viewType()).isEqualTo(PortalTodoListViewType.COPIED);
        assertThat(view.todos().pagination().total()).isEqualTo(1);
        assertThat(view.todos().items())
                .extracting(item -> item.todoId() + ":" + item.status())
                .containsExactly("copied-1:UNREAD");
    }

    @Test
    void shouldReturnEmptyTodoPageWhenKeywordDoesNotMatch() {
        PortalTodoListAggregationApplicationService service = new PortalTodoListAggregationApplicationService(
                todoQueryApplicationService(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );

        PortalTodoListView view = service.officeCenterTodos(
                1,
                20,
                PortalTodoListViewType.COMPLETED,
                null,
                false,
                "missing",
                null
        );

        assertThat(view.todos().items()).isEmpty();
        assertThat(view.todos().pagination().total()).isEqualTo(0);
        assertThat(view.todos().pagination().totalPages()).isEqualTo(0);
    }

    private TodoQueryApplicationService todoQueryApplicationService() {
        InMemoryTodoItemRepository repository = new InMemoryTodoItemRepository();
        repository.save(new TodoItem(
                "todo-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "approval",
                "Approve travel request",
                "HIGH",
                TodoItemStatus.PENDING,
                FIXED_TIME.plusSeconds(600),
                FIXED_TIME.minusSeconds(60),
                FIXED_TIME.minusSeconds(300),
                FIXED_TIME.minusSeconds(60),
                null,
                null
        ));
        repository.save(new TodoItem(
                "todo-2",
                "task-2",
                "instance-2",
                "assignment-1",
                "APPROVAL",
                "approval",
                "Review contract",
                "NORMAL",
                TodoItemStatus.PENDING,
                FIXED_TIME.plusSeconds(1200),
                null,
                FIXED_TIME.minusSeconds(600),
                FIXED_TIME.minusSeconds(120),
                null,
                null
        ));
        repository.save(new TodoItem(
                "todo-3",
                "task-3",
                "instance-3",
                "assignment-1",
                "APPROVAL",
                "approval",
                "Completed request",
                "LOW",
                TodoItemStatus.COMPLETED,
                null,
                null,
                FIXED_TIME.minusSeconds(900),
                FIXED_TIME.minusSeconds(180),
                FIXED_TIME.minusSeconds(180),
                null
        ));

        InMemoryCopiedTodoRepository copiedTodoRepository = new InMemoryCopiedTodoRepository();
        copiedTodoRepository.save(CopiedTodoItem.unread(
                "copied-1",
                "task-4",
                "instance-4",
                "assignment-1",
                "APPROVAL",
                "approval",
                "Copied travel notice",
                "HIGH",
                FIXED_TIME.minusSeconds(90)
        ));
        copiedTodoRepository.save(CopiedTodoItem.unread(
                "copied-2",
                "task-5",
                "instance-5",
                "assignment-1",
                "APPROVAL",
                "approval",
                "Read copied notice",
                "NORMAL",
                FIXED_TIME.minusSeconds(120)
        ).markRead(FIXED_TIME.minusSeconds(30)));

        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        return new TodoQueryApplicationService(
                repository,
                copiedTodoRepository,
                new InMemoryTodoProcessViewRepository(),
                identityContextProvider
        );
    }
}
