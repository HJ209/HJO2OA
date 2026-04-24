package com.hjo2oa.data.openapi.infrastructure;

import com.hjo2oa.data.openapi.domain.AuthenticatedOpenApiInvocationContext;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OpenApiInvocationContextHolder {

    private final ThreadLocal<AuthenticatedOpenApiInvocationContext> contextHolder = new ThreadLocal<>();

    public void set(AuthenticatedOpenApiInvocationContext context) {
        contextHolder.set(context);
    }

    public Optional<AuthenticatedOpenApiInvocationContext> current() {
        return Optional.ofNullable(contextHolder.get());
    }

    public AuthenticatedOpenApiInvocationContext getRequired() {
        return current().orElseThrow(() -> new BizException(
                SharedErrorDescriptors.UNAUTHORIZED,
                "Current request is not authenticated as OpenApi invocation"
        ));
    }

    public void clear() {
        contextHolder.remove();
    }
}
