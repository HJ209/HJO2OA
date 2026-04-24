package com.hjo2oa.data.data.sync.domain;

import java.util.List;

public record PagedResult<T>(
        List<T> items,
        int page,
        int size,
        long total
) {

    public PagedResult {
        items = items == null ? List.of() : List.copyOf(items);
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
    }

    public long totalPages() {
        return total == 0 ? 0 : (total + size - 1L) / size;
    }
}
