package com.hjo2oa.todo.center.domain;

import java.util.List;

public record TodoBatchActionResult(
        int requestedCount,
        int successCount,
        int skippedCount,
        List<String> succeededTodoIds,
        List<String> skippedTodoIds
) {

    public TodoBatchActionResult {
        succeededTodoIds = succeededTodoIds == null ? List.of() : List.copyOf(succeededTodoIds);
        skippedTodoIds = skippedTodoIds == null ? List.of() : List.copyOf(skippedTodoIds);
    }
}
