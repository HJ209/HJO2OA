package com.hjo2oa.portal.aggregation.api.infrastructure;

import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCardDataProvider;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoItem;
import com.hjo2oa.todo.center.application.TodoQueryApplicationService;
import com.hjo2oa.todo.center.domain.TodoItemSummary;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TodoCenterPortalTodoCardDataProvider implements PortalTodoCardDataProvider {

    private static final int DEFAULT_TOP_ITEM_LIMIT = 5;

    private final TodoQueryApplicationService todoQueryApplicationService;

    public TodoCenterPortalTodoCardDataProvider(TodoQueryApplicationService todoQueryApplicationService) {
        this.todoQueryApplicationService = todoQueryApplicationService;
    }

    @Override
    public PortalTodoCard currentTodoCard() {
        List<TodoItemSummary> pendingTodos = todoQueryApplicationService.pendingTodos();
        Map<String, Long> categoryStats = pendingTodos.stream()
                .collect(Collectors.groupingBy(TodoItemSummary::category, Collectors.counting()));
        long urgentCount = pendingTodos.stream()
                .filter(this::isUrgent)
                .count();

        List<PortalTodoItem> topItems = pendingTodos.stream()
                .limit(DEFAULT_TOP_ITEM_LIMIT)
                .map(todo -> new PortalTodoItem(
                        todo.todoId(),
                        todo.title(),
                        todo.category(),
                        todo.urgency(),
                        todo.dueTime(),
                        todo.createdAt()
                ))
                .toList();

        return new PortalTodoCard(pendingTodos.size(), urgentCount, categoryStats, topItems);
    }

    private boolean isUrgent(TodoItemSummary todoItemSummary) {
        String urgency = todoItemSummary.urgency();
        if (urgency == null) {
            return false;
        }
        return "HIGH".equalsIgnoreCase(urgency) || "CRITICAL".equalsIgnoreCase(urgency);
    }
}
