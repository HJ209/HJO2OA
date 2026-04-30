package com.hjo2oa.todo.center.application;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import com.hjo2oa.todo.center.domain.TodoActionLogRepository;
import com.hjo2oa.todo.center.domain.TodoBatchActionResult;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemCompletedEvent;
import com.hjo2oa.todo.center.domain.TodoItemRemindedEvent;
import com.hjo2oa.todo.center.domain.TodoItemRepository;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.domain.TodoItemSummary;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TodoActionApplicationService {

    private final TodoItemRepository todoItemRepository;
    private final TodoActionLogRepository actionLogRepository;
    private final TodoIdentityContextProvider identityContextProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    @Autowired
    public TodoActionApplicationService(
            TodoItemRepository todoItemRepository,
            TodoActionLogRepository actionLogRepository,
            TodoIdentityContextProvider identityContextProvider,
            DomainEventPublisher domainEventPublisher
    ) {
        this(todoItemRepository, actionLogRepository, identityContextProvider, domainEventPublisher, Clock.systemUTC());
    }

    public TodoActionApplicationService(
            TodoItemRepository todoItemRepository,
            TodoActionLogRepository actionLogRepository,
            TodoIdentityContextProvider identityContextProvider,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.todoItemRepository = Objects.requireNonNull(todoItemRepository, "todoItemRepository must not be null");
        this.actionLogRepository = Objects.requireNonNull(actionLogRepository, "actionLogRepository must not be null");
        this.identityContextProvider = Objects.requireNonNull(
                identityContextProvider,
                "identityContextProvider must not be null"
        );
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public TodoItemSummary detail(String todoId) {
        TodoItem todoItem = loadVisiblePendingOrCompleted(todoId);
        return TodoItemSummary.from(todoItem);
    }

    public TodoItemSummary complete(String todoId, String idempotencyKey) {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        Instant now = now();
        if (!actionLogRepository.registerIfAbsent(requireText(idempotencyKey), "COMPLETE", todoId, now)) {
            return TodoItemSummary.from(loadVisiblePendingOrCompleted(todoId));
        }
        TodoItem todoItem = loadVisible(todoId, identityContext);
        if (todoItem.status() == TodoItemStatus.COMPLETED) {
            return TodoItemSummary.from(todoItem);
        }
        if (todoItem.status() != TodoItemStatus.PENDING) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Only pending todo can be completed");
        }
        TodoItem completed = todoItemRepository.save(todoItem.complete(now));
        domainEventPublisher.publish(TodoItemCompletedEvent.from(completed, now, identityContext.tenantId()));
        return TodoItemSummary.from(completed);
    }

    public TodoBatchActionResult batchComplete(List<String> todoIds, String idempotencyKey) {
        List<String> requested = normalizeTodoIds(todoIds);
        if (!actionLogRepository.registerIfAbsent(requireText(idempotencyKey), "BATCH_COMPLETE",
                String.join(",", requested), now())) {
            return new TodoBatchActionResult(requested.size(), 0, requested.size(), List.of(), requested);
        }
        List<String> succeeded = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (String todoId : requested) {
            try {
                complete(todoId, idempotencyKey + ":" + todoId);
                succeeded.add(todoId);
            } catch (BizException ex) {
                skipped.add(todoId);
            }
        }
        return new TodoBatchActionResult(requested.size(), succeeded.size(), skipped.size(), succeeded, skipped);
    }

    public TodoItemSummary remind(String todoId, String reason, String idempotencyKey) {
        TodoIdentityContext identityContext = identityContextProvider.currentContext();
        Instant now = now();
        if (!actionLogRepository.registerIfAbsent(requireText(idempotencyKey), "REMIND", todoId, now)) {
            return TodoItemSummary.from(loadVisiblePendingOrCompleted(todoId));
        }
        TodoItem todoItem = loadVisible(todoId, identityContext);
        if (todoItem.status() != TodoItemStatus.PENDING) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Only pending todo can be reminded");
        }
        domainEventPublisher.publish(TodoItemRemindedEvent.from(todoItem, now, reason));
        return TodoItemSummary.from(todoItem);
    }

    public TodoBatchActionResult batchRemind(List<String> todoIds, String reason, String idempotencyKey) {
        List<String> requested = normalizeTodoIds(todoIds);
        if (!actionLogRepository.registerIfAbsent(requireText(idempotencyKey), "BATCH_REMIND",
                String.join(",", requested), now())) {
            return new TodoBatchActionResult(requested.size(), 0, requested.size(), List.of(), requested);
        }
        List<String> succeeded = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (String todoId : requested) {
            try {
                remind(todoId, reason, idempotencyKey + ":" + todoId);
                succeeded.add(todoId);
            } catch (BizException ex) {
                skipped.add(todoId);
            }
        }
        return new TodoBatchActionResult(requested.size(), succeeded.size(), skipped.size(), succeeded, skipped);
    }

    private TodoItem loadVisiblePendingOrCompleted(String todoId) {
        return loadVisible(todoId, identityContextProvider.currentContext());
    }

    private TodoItem loadVisible(String todoId, TodoIdentityContext identityContext) {
        return todoItemRepository.findByTodoId(requireText(todoId))
                .filter(todo -> todo.tenantId().equals(identityContext.tenantId()))
                .filter(todo -> todo.assigneeId().equals(identityContext.assignmentId()))
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Todo item not found"));
    }

    private List<String> normalizeTodoIds(List<String> todoIds) {
        if (todoIds == null || todoIds.isEmpty()) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "todoIds must not be empty");
        }
        return todoIds.stream()
                .map(this::requireText)
                .distinct()
                .toList();
    }

    private String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "value must not be blank");
        }
        return value.trim();
    }

    private Instant now() {
        return clock.instant();
    }
}
