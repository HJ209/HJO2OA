package com.hjo2oa.shared.web;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SharedRequestContextFactory {

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    public static final String TIMEZONE_HEADER = "X-Timezone";
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private static final String DEFAULT_LANGUAGE = "zh-CN";
    private static final String DEFAULT_TIMEZONE = "UTC";

    public SharedRequestContext create(HttpServletRequest request) {
        String requestId = normalizedHeader(request, ResponseMetaFactory.REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return new SharedRequestContext(
                resolveTenantId(normalizedHeader(request, TENANT_ID_HEADER)),
                requestId,
                resolveLanguage(normalizedHeader(request, ACCEPT_LANGUAGE_HEADER)),
                resolveTimezone(normalizedHeader(request, TIMEZONE_HEADER)),
                normalizedHeader(request, IDEMPOTENCY_KEY_HEADER)
        );
    }

    private static String resolveTenantId(String rawTenantId) {
        if (rawTenantId == null) {
            return null;
        }
        try {
            return UUID.fromString(rawTenantId).toString();
        } catch (IllegalArgumentException ex) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "X-Tenant-Id must be a UUID", ex);
        }
    }

    private static String resolveLanguage(String rawLanguage) {
        if (rawLanguage == null) {
            return DEFAULT_LANGUAGE;
        }
        String languageTag = rawLanguage.split(",", 2)[0].trim();
        if (languageTag.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        Locale locale = Locale.forLanguageTag(languageTag);
        if (locale.getLanguage().isBlank()) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Accept-Language is invalid");
        }
        return locale.toLanguageTag();
    }

    private static String resolveTimezone(String rawTimezone) {
        if (rawTimezone == null) {
            return DEFAULT_TIMEZONE;
        }
        try {
            return ZoneId.of(rawTimezone).getId();
        } catch (ZoneRulesException | IllegalArgumentException ex) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "X-Timezone is invalid");
        }
    }

    private static String normalizedHeader(HttpServletRequest request, String headerName) {
        if (request == null) {
            return null;
        }
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
