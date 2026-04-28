package com.hjo2oa.todo.center.application;

import com.hjo2oa.todo.center.domain.CopiedTodoItem;
import com.hjo2oa.todo.center.domain.CopiedTodoRepository;
import com.hjo2oa.todo.center.domain.CopiedTodoSummary;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class CopiedTodoActionApplicationService {

    private final CopiedTodoRepository copiedTodoRepository;
    private final TodoIdentityContextProvider identityContextProvider;
    private final Clock clock;
    @Autowired
    public CopiedTodoActionApplicationService(
            CopiedTodoRepository copiedTodoRepository,
            TodoIdentityContextProvider identityContextProvider
    ) {
        this(copiedTodoRepository, identityContextProvider, Clock.systemUTC());
    }
    public CopiedTodoActionApplicationService(
            CopiedTodoRepository copiedTodoRepository,
            TodoIdentityContextProvider identityContextProvider,
            Clock clock
    ) {
        this.copiedTodoRepository = Objects.requireNonNull(copiedTodoRepository, "copiedTodoRepository must not be null");
        this.identityContextProvider = Objects.requireNonNull(
                identityContextProvider,
                "identityContextProvider must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Optional<CopiedTodoSummary> markRead(String todoId) {
        Objects.requireNonNull(todoId, "todoId must not be null");
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        Optional<CopiedTodoItem> visibleCopiedTodo = copiedTodoRepository.findByTodoId(todoId)
                .filter(copiedTodo -> copiedTodo.isVisibleTo(identityContext));

        if (visibleCopiedTodo.isEmpty()) {
            return Optional.empty();
        }

        CopiedTodoItem copiedTodo = visibleCopiedTodo.orElseThrow();
        if (copiedTodo.isUnread()) {
            copiedTodo = copiedTodoRepository.save(copiedTodo.markRead(clock.instant()));
        }

        return Optional.of(CopiedTodoSummary.from(copiedTodo));
    }
}
