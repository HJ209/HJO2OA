package com.hjo2oa.portal.aggregation.api.application;

import com.hjo2oa.portal.aggregation.api.domain.PortalTodoListItem;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoListSummary;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoListView;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoListViewType;
import com.hjo2oa.shared.web.PageData;
import com.hjo2oa.shared.web.Pagination;
import com.hjo2oa.todo.center.application.TodoQueryApplicationService;
import com.hjo2oa.todo.center.domain.CopiedTodoReadStatus;
import com.hjo2oa.todo.center.domain.CopiedTodoSummary;
import com.hjo2oa.todo.center.domain.TodoCounts;
import com.hjo2oa.todo.center.domain.TodoItemSummary;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PortalTodoListAggregationApplicationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TodoQueryApplicationService todoQueryApplicationService;
    private final Clock clock;

    public PortalTodoListAggregationApplicationService(TodoQueryApplicationService todoQueryApplicationService) {
        this(todoQueryApplicationService, Clock.systemUTC());
    }

    public PortalTodoListAggregationApplicationService(
            TodoQueryApplicationService todoQueryApplicationService,
            Clock clock
    ) {
        this.todoQueryApplicationService = Objects.requireNonNull(
                todoQueryApplicationService,
                "todoQueryApplicationService must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PortalTodoListView officeCenterTodos(
            int page,
            int size,
            PortalTodoListViewType viewType,
            String todoCategory,
            boolean urgentOnly,
            String keyword,
            CopiedTodoReadStatus copiedReadStatus
    ) {
        int normalizedPage = validatePage(page);
        int normalizedSize = validateSize(size);
        PortalTodoListViewType normalizedViewType = viewType == null ? PortalTodoListViewType.PENDING : viewType;
        String normalizedCategory = normalizeFilter(todoCategory);
        String normalizedKeyword = normalizeFilter(keyword);

        List<PortalTodoListItem> filteredTodos = switch (normalizedViewType) {
            case PENDING -> todoQueryApplicationService.pendingTodos().stream()
                    .map(this::toPendingTodoListItem)
                    .filter(item -> matchesCategory(item, normalizedCategory))
                    .filter(item -> matchesUrgency(item, urgentOnly))
                    .filter(item -> matchesKeyword(item, normalizedKeyword))
                    .toList();
            case COMPLETED -> todoQueryApplicationService.completedTodos().stream()
                    .map(this::toCompletedTodoListItem)
                    .filter(item -> matchesCategory(item, normalizedCategory))
                    .filter(item -> matchesUrgency(item, urgentOnly))
                    .filter(item -> matchesKeyword(item, normalizedKeyword))
                    .toList();
            case COPIED -> todoQueryApplicationService.copiedTodos(copiedReadStatus).stream()
                    .map(this::toCopiedTodoListItem)
                    .filter(item -> matchesCategory(item, normalizedCategory))
                    .filter(item -> matchesUrgency(item, urgentOnly))
                    .filter(item -> matchesKeyword(item, normalizedKeyword))
                    .toList();
        };

        long total = filteredTodos.size();
        int startIndex = Math.min((normalizedPage - 1) * normalizedSize, filteredTodos.size());
        int endIndex = Math.min(startIndex + normalizedSize, filteredTodos.size());
        List<PortalTodoListItem> items = filteredTodos.subList(startIndex, endIndex);
        TodoCounts counts = todoQueryApplicationService.counts();

        return new PortalTodoListView(
                normalizedViewType,
                new PortalTodoListSummary(
                        counts.pendingCount(),
                        counts.completedCount(),
                        counts.overdueCount(),
                        counts.copiedUnreadCount()
                ),
                new PageData<>(items, Pagination.of(normalizedPage, normalizedSize, total)),
                clock.instant()
        );
    }

    private int validatePage(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than 0");
        }
        return page;
    }

    private int validateSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size exceeds max page size " + MAX_PAGE_SIZE);
        }
        return size;
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private boolean matchesCategory(PortalTodoListItem item, String todoCategory) {
        if (todoCategory == null) {
            return true;
        }
        return item.category() != null && item.category().toLowerCase(Locale.ROOT).equals(todoCategory);
    }

    private boolean matchesUrgency(PortalTodoListItem item, boolean urgentOnly) {
        if (!urgentOnly) {
            return true;
        }
        String urgency = item.urgency();
        if (urgency == null) {
            return false;
        }
        String normalizedUrgency = urgency.toUpperCase(Locale.ROOT);
        return normalizedUrgency.equals("HIGH")
                || normalizedUrgency.equals("URGENT")
                || normalizedUrgency.equals("CRITICAL");
    }

    private boolean matchesKeyword(PortalTodoListItem item, String keyword) {
        if (keyword == null) {
            return true;
        }
        return containsIgnoreCase(item.title(), keyword);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        if (text == null) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private PortalTodoListItem toPendingTodoListItem(TodoItemSummary summary) {
        return toTodoListItem(summary, PortalTodoListViewType.PENDING);
    }

    private PortalTodoListItem toCompletedTodoListItem(TodoItemSummary summary) {
        return toTodoListItem(summary, PortalTodoListViewType.COMPLETED);
    }

    private PortalTodoListItem toTodoListItem(TodoItemSummary summary, PortalTodoListViewType viewType) {
        return new PortalTodoListItem(
                summary.todoId(),
                summary.taskId(),
                summary.instanceId(),
                summary.title(),
                summary.category(),
                summary.urgency(),
                viewType,
                summary.status().name(),
                summary.dueTime(),
                summary.overdueAt(),
                summary.createdAt(),
                summary.updatedAt(),
                summary.completedAt(),
                null
        );
    }

    private PortalTodoListItem toCopiedTodoListItem(CopiedTodoSummary summary) {
        return new PortalTodoListItem(
                summary.todoId(),
                summary.taskId(),
                summary.instanceId(),
                summary.title(),
                summary.category(),
                summary.urgency(),
                PortalTodoListViewType.COPIED,
                summary.readStatus().name(),
                null,
                null,
                summary.createdAt(),
                summary.updatedAt(),
                null,
                summary.readAt()
        );
    }
}
