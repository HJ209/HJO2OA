package com.hjo2oa.shared.web;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

@FunctionalInterface
public interface ErrorMessageResolver {

    Optional<String> resolve(
            ErrorDescriptor descriptor,
            String fallbackMessage,
            HttpServletRequest request
    );

    static ErrorMessageResolver noop() {
        return (descriptor, fallbackMessage, request) -> Optional.empty();
    }
}
