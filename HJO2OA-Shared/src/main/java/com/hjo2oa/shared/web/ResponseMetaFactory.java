package com.hjo2oa.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResponseMetaFactory {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final ZoneId SERVER_TIMEZONE = ZoneId.of("UTC");

    private final Clock clock;
    private final SharedRequestContextFactory requestContextFactory;

    @Autowired
    public ResponseMetaFactory() {
        this(Clock.systemUTC(), new SharedRequestContextFactory());
    }

    public ResponseMetaFactory(Clock clock) {
        this(clock, new SharedRequestContextFactory());
    }

    ResponseMetaFactory(Clock clock, SharedRequestContextFactory requestContextFactory) {
        this.clock = clock;
        this.requestContextFactory = requestContextFactory;
    }

    public ResponseMeta create(HttpServletRequest request) {
        SharedRequestContext context = SharedRequestContextHolder.current()
                .orElseGet(() -> createRequestContextSafely(request));
        return new ResponseMeta(
                context.requestId(),
                clock.instant(),
                SERVER_TIMEZONE.getId(),
                context.tenantId(),
                context.language(),
                context.timezone(),
                context.idempotencyKey()
        );
    }

    private SharedRequestContext createRequestContextSafely(HttpServletRequest request) {
        try {
            return requestContextFactory.create(request);
        } catch (RuntimeException ex) {
            return new SharedRequestContext(
                    null,
                    resolveRequestId(request),
                    "zh-CN",
                    SERVER_TIMEZONE.getId(),
                    null
            );
        }
    }

    private static String resolveRequestId(HttpServletRequest request) {
        if (request == null) {
            return UUID.randomUUID().toString();
        }
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }
}
