package com.hjo2oa.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SharedWebContractInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATION_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final SharedRequestContextFactory contextFactory;
    private final IdempotencyRegistry idempotencyRegistry;
    private final Clock clock;

    @Autowired
    public SharedWebContractInterceptor(
            SharedRequestContextFactory contextFactory,
            IdempotencyRegistry idempotencyRegistry
    ) {
        this(contextFactory, idempotencyRegistry, Clock.systemUTC());
    }

    public SharedWebContractInterceptor(
            SharedRequestContextFactory contextFactory,
            IdempotencyRegistry idempotencyRegistry,
            Clock clock
    ) {
        this.contextFactory = contextFactory;
        this.idempotencyRegistry = idempotencyRegistry;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!usesSharedContract(handler)) {
            return true;
        }
        SharedRequestContext context = contextFactory.create(request);
        SharedRequestContextHolder.set(context);
        request.setAttribute(SharedRequestContext.ATTRIBUTE_NAME, context);
        response.setHeader(ResponseMetaFactory.REQUEST_ID_HEADER, context.requestId());
        registerIdempotency(request, context);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        if (usesSharedContract(handler)) {
            SharedRequestContextHolder.clear();
        }
    }

    private void registerIdempotency(HttpServletRequest request, SharedRequestContext context) {
        if (!MUTATION_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            return;
        }
        idempotencyRegistry.register(
                context.tenantId(),
                context.idempotencyKey(),
                fingerprint(request),
                clock.instant()
        );
    }

    private static boolean usesSharedContract(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.getBeanType().isAnnotationPresent(UseSharedWebContract.class);
        }
        return false;
    }

    private static String fingerprint(HttpServletRequest request) {
        String queryString = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        return request.getMethod() + " " + request.getRequestURI() + queryString;
    }
}
