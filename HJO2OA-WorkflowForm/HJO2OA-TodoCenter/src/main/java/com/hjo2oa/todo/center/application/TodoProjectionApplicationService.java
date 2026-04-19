package com.hjo2oa.todo.center.application;

import com.hjo2oa.shared.messaging.DomainEventPublisher;
import com.hjo2oa.todo.center.domain.ProcessTaskCompletedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskCreatedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskOverdueEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskTerminatedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskTransferredEvent;
import com.hjo2oa.todo.center.domain.TodoItem;
import com.hjo2oa.todo.center.domain.TodoItemCancelledEvent;
import com.hjo2oa.todo.center.domain.TodoItemCompletedEvent;
import com.hjo2oa.todo.center.domain.TodoItemCreatedEvent;
import com.hjo2oa.todo.center.domain.TodoItemOverdueEvent;
import com.hjo2oa.todo.center.domain.TodoItemRepository;
import com.hjo2oa.todo.center.domain.TodoItemStatus;
import com.hjo2oa.todo.center.domain.TodoProjectionEventLog;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TodoProjectionApplicationService {

    private final TodoItemRepository todoItemRepository;
    private final TodoProjectionEventLog projectionEventLog;
    private final DomainEventPublisher domainEventPublisher;

    public TodoProjectionApplicationService(
            TodoItemRepository todoItemRepository,
            TodoProjectionEventLog projectionEventLog,
            DomainEventPublisher domainEventPublisher
    ) {
        this.todoItemRepository = todoItemRepository;
        this.projectionEventLog = projectionEventLog;
        this.domainEventPublisher = domainEventPublisher;
    }

    public void onTaskCreated(ProcessTaskCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        if (!projectionEventLog.registerIfAbsent(event.eventId())) {
            return;
        }

        TodoItem todoItem = todoItemRepository.findByTaskId(event.taskId())
                .map(existing -> existing.refreshFrom(event))
                .orElseGet(() -> TodoItem.create(UUID.randomUUID().toString(), event));

        boolean created = todoItemRepository.findByTaskId(event.taskId()).isEmpty();
        todoItemRepository.save(todoItem);
        if (created) {
            domainEventPublisher.publish(TodoItemCreatedEvent.from(todoItem, event.occurredAt(), event.tenantId()));
        }
    }

    public void onTaskCompleted(ProcessTaskCompletedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        if (!projectionEventLog.registerIfAbsent(event.eventId())) {
            return;
        }

        todoItemRepository.findByTaskId(event.taskId())
                .map(todoItem -> todoItem.complete(event.completedTime()))
                .ifPresent(todoItem -> {
                    todoItemRepository.save(todoItem);
                    domainEventPublisher.publish(TodoItemCompletedEvent.from(todoItem, event.occurredAt(), event.tenantId()));
                });
    }

    public void onTaskTerminated(ProcessTaskTerminatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        if (!projectionEventLog.registerIfAbsent(event.eventId())) {
            return;
        }

        todoItemRepository.findByTaskId(event.taskId())
                .map(todoItem -> todoItem.cancel(event.reason(), event.occurredAt()))
                .ifPresent(todoItem -> {
                    todoItemRepository.save(todoItem);
                    domainEventPublisher.publish(TodoItemCancelledEvent.from(todoItem, event.occurredAt(), event.tenantId()));
                });
    }

    public void onTaskTransferred(ProcessTaskTransferredEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        if (!projectionEventLog.registerIfAbsent(event.eventId())) {
            return;
        }

        todoItemRepository.findByTaskId(event.taskId())
                .map(todoItem -> todoItem.transferTo(event.toAssigneeId(), event.occurredAt()))
                .ifPresent(todoItemRepository::save);
    }

    public void onTaskOverdue(ProcessTaskOverdueEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        if (!projectionEventLog.registerIfAbsent(event.eventId())) {
            return;
        }

        todoItemRepository.findByTaskId(event.taskId())
                .filter(todoItem -> todoItem.status() == TodoItemStatus.PENDING)
                .filter(todoItem -> todoItem.dueTime() != null)
                .filter(todoItem -> !todoItem.isOverdue())
                .map(todoItem -> todoItem.markOverdue(event.occurredAt()))
                .ifPresent(todoItem -> {
                    todoItemRepository.save(todoItem);
                    domainEventPublisher.publish(TodoItemOverdueEvent.from(todoItem, event.occurredAt(), event.tenantId()));
                });
    }
}
