package com.hjo2oa.shared.kernel;

import java.util.Objects;
import org.springframework.http.HttpStatus;

public record ErrorDescriptor(
        String code,
        HttpStatus httpStatus,
        String defaultMessage
) {

    public ErrorDescriptor {
        Objects.requireNonNull(httpStatus, "httpStatus must not be null");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (defaultMessage == null || defaultMessage.isBlank()) {
            throw new IllegalArgumentException("defaultMessage must not be blank");
        }
    }
}
