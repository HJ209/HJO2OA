package com.hjo2oa.todo.center.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hjo2oa.todo.center.domain.CopiedTodoItem;
import com.hjo2oa.todo.center.domain.CopiedTodoReadStatus;
import com.hjo2oa.todo.center.domain.CopiedTodoSummary;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import com.hjo2oa.todo.center.infrastructure.InMemoryCopiedTodoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CopiedTodoActionApplicationServiceTest {

    @Test
    void shouldMarkCopiedTodoAsReadIdempotently() {
        InMemoryCopiedTodoRepository repository = new InMemoryCopiedTodoRepository();
        repository.save(CopiedTodoItem.unread(
                "copied-1",
                "task-1",
                "instance-1",
                "assignment-1",
                "APPROVAL",
                "EXPENSE",
                "Read copied todo",
                "HIGH",
                Instant.parse("2026-04-19T09:00:00Z")
        ));
        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        CopiedTodoActionApplicationService service = new CopiedTodoActionApplicationService(
                repository,
                identityContextProvider,
                Clock.fixed(Instant.parse("2026-04-19T10:00:00Z"), ZoneOffset.UTC)
        );

        Optional<CopiedTodoSummary> firstRead = service.markRead("copied-1");
        Optional<CopiedTodoSummary> secondRead = service.markRead("copied-1");

        assertTrue(firstRead.isPresent());
        assertEquals(CopiedTodoReadStatus.READ, firstRead.orElseThrow().readStatus());
        assertNotNull(firstRead.orElseThrow().readAt());
        assertTrue(secondRead.isPresent());
        assertEquals(firstRead.orElseThrow().readAt(), secondRead.orElseThrow().readAt());
    }

    @Test
    void shouldReturnEmptyWhenCopiedTodoIsNotVisibleToCurrentIdentity() {
        InMemoryCopiedTodoRepository repository = new InMemoryCopiedTodoRepository();
        repository.save(CopiedTodoItem.unread(
                "copied-2",
                "task-2",
                "instance-2",
                "assignment-2",
                "APPROVAL",
                "PROCUREMENT",
                "Invisible copied todo",
                "NORMAL",
                Instant.parse("2026-04-19T09:30:00Z")
        ));
        TodoIdentityContextProvider identityContextProvider = () -> new TodoIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        CopiedTodoActionApplicationService service = new CopiedTodoActionApplicationService(
                repository,
                identityContextProvider,
                Clock.fixed(Instant.parse("2026-04-19T10:00:00Z"), ZoneOffset.UTC)
        );

        Optional<CopiedTodoSummary> readResult = service.markRead("copied-2");

        assertFalse(readResult.isPresent());
    }
}
