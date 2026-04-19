package com.hjo2oa.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorDetail(
        String field,
        String message,
        Object rejectedValue
) {
}
