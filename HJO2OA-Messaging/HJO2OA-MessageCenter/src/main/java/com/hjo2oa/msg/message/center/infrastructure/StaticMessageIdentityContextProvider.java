package com.hjo2oa.msg.message.center.infrastructure;

import com.hjo2oa.msg.message.center.domain.MessageIdentityContext;
import com.hjo2oa.msg.message.center.domain.MessageIdentityContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class StaticMessageIdentityContextProvider implements MessageIdentityContextProvider {

    private static final String DEFAULT_TENANT_ID = "tenant-1";
    private static final String DEFAULT_RECIPIENT_ID = "assignment-1";
    private static final String DEFAULT_ASSIGNMENT_ID = "assignment-1";
    private static final String DEFAULT_POSITION_ID = "position-1";

    @Override
    public MessageIdentityContext currentContext() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return fallbackContext();
        }
        return new MessageIdentityContext(
                headerOrDefault(request, "X-Tenant-Id", DEFAULT_TENANT_ID),
                resolveRecipientId(request),
                headerOrDefault(request, "X-Identity-Assignment-Id", DEFAULT_ASSIGNMENT_ID),
                headerOrDefault(request, "X-Identity-Position-Id", DEFAULT_POSITION_ID)
        );
    }

    private MessageIdentityContext fallbackContext() {
        return new MessageIdentityContext(
                DEFAULT_TENANT_ID,
                DEFAULT_RECIPIENT_ID,
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

    private String resolveRecipientId(HttpServletRequest request) {
        String personId = request.getHeader("X-Person-Id");
        if (personId != null && !personId.isBlank()) {
            return personId;
        }
        String accountPersonId = request.getHeader("X-Operator-Person-Id");
        if (accountPersonId != null && !accountPersonId.isBlank()) {
            return accountPersonId;
        }
        return headerOrDefault(request, "X-Identity-Assignment-Id", DEFAULT_RECIPIENT_ID);
    }

    private String headerOrDefault(HttpServletRequest request, String headerName, String defaultValue) {
        String value = request.getHeader(headerName);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
