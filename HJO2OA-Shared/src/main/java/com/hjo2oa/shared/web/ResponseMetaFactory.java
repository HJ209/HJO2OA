package com.hjo2oa.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class ResponseMetaFactory {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final ZoneId SERVER_TIMEZONE = ZoneId.of("UTC");

    private final Clock clock;

    @Autowired

    public ResponseMetaFactory() {
        this(Clock.systemUTC());
    }

    public ResponseMetaFactory(Clock clock) {
        this.clock = clock;
    }

    public ResponseMeta create(HttpServletRequest request) {
        String requestId = request == null ? null : request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return new ResponseMeta(requestId, clock.instant(), SERVER_TIMEZONE.getId());
    }
}
