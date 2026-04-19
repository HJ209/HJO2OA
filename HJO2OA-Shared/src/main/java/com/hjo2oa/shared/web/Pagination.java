package com.hjo2oa.shared.web;

public record Pagination(
        int page,
        int size,
        long total,
        long totalPages
) {

    public Pagination {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("totalPages must not be negative");
        }
    }

    public static Pagination of(int page, int size, long total) {
        long totalPages = total == 0 ? 0 : (total + size - 1) / size;
        return new Pagination(page, size, total, totalPages);
    }
}
