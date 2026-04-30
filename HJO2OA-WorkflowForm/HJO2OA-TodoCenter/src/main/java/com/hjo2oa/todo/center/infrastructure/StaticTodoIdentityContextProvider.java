package com.hjo2oa.todo.center.infrastructure;

import com.hjo2oa.shared.tenant.SharedTenant;
import com.hjo2oa.todo.center.domain.TodoIdentityContext;
import com.hjo2oa.todo.center.domain.TodoIdentityContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class StaticTodoIdentityContextProvider implements TodoIdentityContextProvider {

    private static final String DEFAULT_TENANT_ID = "tenant-1";
    private static final String DEFAULT_PERSON_ID = "person-1";
    private static final String DEFAULT_ASSIGNMENT_ID = "assignment-1";
    private static final String DEFAULT_POSITION_ID = "position-1";

    @Override
    public TodoIdentityContext currentContext() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return fallbackContext();
        }
        return new TodoIdentityContext(
                headerOrDefault(request, SharedTenant.TENANT_ID_HEADER, DEFAULT_TENANT_ID),
                resolvePersonId(request),
                headerOrDefault(request, SharedTenant.IDENTITY_ASSIGNMENT_ID_HEADER, DEFAULT_ASSIGNMENT_ID),
                headerOrDefault(request, SharedTenant.IDENTITY_POSITION_ID_HEADER, DEFAULT_POSITION_ID)
        );
    }

    private TodoIdentityContext fallbackContext() {
        return new TodoIdentityContext(
                DEFAULT_TENANT_ID,
                DEFAULT_PERSON_ID,
                DEFAULT_ASSIGNMENT_ID,
                DEFAULT_POSITION_ID
        );
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String resolvePersonId(HttpServletRequest request) {
        String personId = request.getHeader("X-Person-Id");
        if (personId != null && !personId.isBlank()) {
            return personId.trim();
        }
        String operatorPersonId = request.getHeader("X-Operator-Person-Id");
        if (operatorPersonId != null && !operatorPersonId.isBlank()) {
            return operatorPersonId.trim();
        }
        return DEFAULT_PERSON_ID;
    }

    private String headerOrDefault(HttpServletRequest request, String headerName, String defaultValue) {
        String value = request.getHeader(headerName);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
