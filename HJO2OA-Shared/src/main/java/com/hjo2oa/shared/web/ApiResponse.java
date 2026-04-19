package com.hjo2oa.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hjo2oa.shared.kernel.ErrorDescriptor;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        List<ApiErrorDetail> errors,
        ResponseMeta meta
) {

    public ApiResponse {
        errors = errors == null ? null : List.copyOf(errors);
    }

    public static <T> ApiResponse<T> success(T data, ResponseMeta meta) {
        return new ApiResponse<>("OK", "操作成功", data, null, meta);
    }

    public static <T> ApiResponse<PageData<T>> page(List<T> items, Pagination pagination, ResponseMeta meta) {
        return new ApiResponse<>("OK", "查询成功", new PageData<>(items, pagination), null, meta);
    }

    public static <T> ApiResponse<T> failure(
            ErrorDescriptor descriptor,
            String message,
            List<ApiErrorDetail> errors,
            ResponseMeta meta
    ) {
        String resolvedMessage = message == null || message.isBlank()
                ? descriptor.defaultMessage()
                : message;
        return new ApiResponse<>(descriptor.code(), resolvedMessage, null, errors, meta);
    }
}
