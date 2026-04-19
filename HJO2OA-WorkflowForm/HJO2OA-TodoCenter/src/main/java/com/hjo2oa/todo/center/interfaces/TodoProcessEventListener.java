package com.hjo2oa.todo.center.interfaces;

import com.hjo2oa.todo.center.application.TodoProjectionApplicationService;
import com.hjo2oa.todo.center.domain.ProcessTaskCompletedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskCreatedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskOverdueEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskTerminatedEvent;
import com.hjo2oa.todo.center.domain.ProcessTaskTransferredEvent;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

@Component
public class TodoProcessEventListener {

    private final TodoProjectionApplicationService todoProjectionApplicationService;

    public TodoProcessEventListener(TodoProjectionApplicationService todoProjectionApplicationService) {
        this.todoProjectionApplicationService = todoProjectionApplicationService;
    }

    @EventListener
    public void onTaskCreated(ProcessTaskCreatedEvent event) {
        todoProjectionApplicationService.onTaskCreated(event);
    }

    @EventListener
    public void onTaskCompleted(ProcessTaskCompletedEvent event) {
        todoProjectionApplicationService.onTaskCompleted(event);
    }

    @EventListener
    public void onTaskTerminated(ProcessTaskTerminatedEvent event) {
        todoProjectionApplicationService.onTaskTerminated(event);
    }

    @EventListener
    public void onTaskTransferred(ProcessTaskTransferredEvent event) {
        todoProjectionApplicationService.onTaskTransferred(event);
    }

    @EventListener
    public void onTaskOverdue(ProcessTaskOverdueEvent event) {
        todoProjectionApplicationService.onTaskOverdue(event);
    }
}
