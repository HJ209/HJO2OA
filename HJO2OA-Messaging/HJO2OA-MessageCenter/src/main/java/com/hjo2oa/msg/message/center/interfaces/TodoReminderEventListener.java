package com.hjo2oa.msg.message.center.interfaces;

import com.hjo2oa.msg.message.center.application.MessageNotificationProjectionApplicationService;
import com.hjo2oa.todo.center.domain.TodoItemCreatedEvent;
import com.hjo2oa.todo.center.domain.TodoItemCompletedEvent;
import com.hjo2oa.todo.center.domain.TodoItemOverdueEvent;
import com.hjo2oa.todo.center.domain.TodoItemRemindedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TodoReminderEventListener {

    private final MessageNotificationProjectionApplicationService projectionApplicationService;

    public TodoReminderEventListener(MessageNotificationProjectionApplicationService projectionApplicationService) {
        this.projectionApplicationService = projectionApplicationService;
    }

    @EventListener
    public void onTodoItemCreated(TodoItemCreatedEvent event) {
        projectionApplicationService.onTodoItemCreated(event);
    }

    @EventListener
    public void onTodoItemOverdue(TodoItemOverdueEvent event) {
        projectionApplicationService.onTodoItemOverdue(event);
    }

    @EventListener
    public void onTodoItemReminded(TodoItemRemindedEvent event) {
        projectionApplicationService.onTodoItemReminded(event);
    }

    @EventListener
    public void onTodoItemCompleted(TodoItemCompletedEvent event) {
        projectionApplicationService.onTodoItemCompleted(event);
    }
}
