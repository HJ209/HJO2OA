package com.hjo2oa.shared.web;

import java.util.List;

public record PageData<T>(
        List<T> items,
        Pagination pagination
) {

    public PageData {
        items = List.copyOf(items);
    }
}
