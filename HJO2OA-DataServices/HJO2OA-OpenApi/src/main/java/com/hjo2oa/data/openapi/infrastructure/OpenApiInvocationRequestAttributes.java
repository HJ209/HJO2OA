package com.hjo2oa.data.openapi.infrastructure;

import com.hjo2oa.data.openapi.domain.ApiInvocationOutcome;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class OpenApiInvocationRequestAttributes {

    public static final String OUTCOME_ATTRIBUTE = "hjo2oa.openapi.outcome";
    public static final String ERROR_CODE_ATTRIBUTE = "hjo2oa.openapi.errorCode";

    private OpenApiInvocationRequestAttributes() {
    }

    public static void mark(ApiInvocationOutcome outcome, String errorCode) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        request.setAttribute(OUTCOME_ATTRIBUTE, outcome);
        if (errorCode != null) {
            request.setAttribute(ERROR_CODE_ATTRIBUTE, errorCode);
        }
    }

    public static Optional<ApiInvocationOutcome> resolveOutcome(HttpServletRequest request) {
        Object value = request.getAttribute(OUTCOME_ATTRIBUTE);
        return value instanceof ApiInvocationOutcome outcome ? Optional.of(outcome) : Optional.empty();
    }

    public static String resolveErrorCode(HttpServletRequest request) {
        Object value = request.getAttribute(ERROR_CODE_ATTRIBUTE);
        return value instanceof String stringValue ? stringValue : null;
    }
}
