package com.hjo2oa.infra.errorcode.infrastructure;

import com.hjo2oa.infra.errorcode.application.ErrorCodeCacheInvalidator;
import com.hjo2oa.infra.errorcode.application.ErrorCodeMessageLocalizer;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinition;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinitionRepository;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinitionView;
import com.hjo2oa.infra.i18n.application.I18nCacheInvalidationListener;
import com.hjo2oa.infra.i18n.application.LocaleBundleApplicationService;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import com.hjo2oa.shared.web.ErrorMessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class ErrorCodeI18nMessageResolver implements ErrorMessageResolver,
        ErrorCodeMessageLocalizer,
        ErrorCodeCacheInvalidator,
        I18nCacheInvalidationListener {

    private static final String ERROR_BUNDLE_CODE = "error.messages";
    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    private static final String DEFAULT_LOCALE = "zh-CN";

    private final ErrorCodeDefinitionRepository errorCodeDefinitionRepository;
    private final LocaleBundleApplicationService localeBundleApplicationService;
    private final ConcurrentMap<MessageCacheKey, String> messageCache = new ConcurrentHashMap<>();

    public ErrorCodeI18nMessageResolver(
            ErrorCodeDefinitionRepository errorCodeDefinitionRepository,
            LocaleBundleApplicationService localeBundleApplicationService
    ) {
        this.errorCodeDefinitionRepository = Objects.requireNonNull(
                errorCodeDefinitionRepository,
                "errorCodeDefinitionRepository must not be null"
        );
        this.localeBundleApplicationService = Objects.requireNonNull(
                localeBundleApplicationService,
                "localeBundleApplicationService must not be null"
        );
    }

    @Override
    public Optional<String> resolve(
            ErrorDescriptor descriptor,
            String fallbackMessage,
            HttpServletRequest request
    ) {
        String locale = resolveLocale(request);
        String fallback = fallbackMessage == null || fallbackMessage.isBlank()
                ? descriptor.defaultMessage()
                : fallbackMessage;
        if (TenantContextHolder.currentTenantId().isEmpty()) {
            return Optional.of(fallback);
        }
        return Optional.of(localizeByCode(descriptor.code(), locale, fallback));
    }

    @Override
    public String localize(ErrorCodeDefinitionView definition, String locale, String fallbackMessage) {
        String fallback = fallbackMessage == null || fallbackMessage.isBlank()
                ? definition.messageKey()
                : fallbackMessage;
        return localizeByMessageKey(definition.messageKey(), normalizeLocale(locale), fallback);
    }

    @Override
    public void invalidateErrorCodeCaches() {
        messageCache.clear();
    }

    @Override
    public void onI18nCacheInvalidated() {
        messageCache.clear();
    }

    private String localizeByCode(String code, String locale, String fallbackMessage) {
        MessageCacheKey cacheKey = new MessageCacheKey(code, normalizeLocale(locale), fallbackMessage);
        return messageCache.computeIfAbsent(cacheKey, key -> localizeByCodeUncached(
                key.code(),
                key.locale(),
                key.fallbackMessage()
        ));
    }

    private String localizeByCodeUncached(String code, String locale, String fallbackMessage) {
        Optional<ErrorCodeDefinition> definition = errorCodeDefinitionRepository.findByCode(code);
        if (definition.isPresent()) {
            return localizeByMessageKey(definition.get().messageKey(), locale, fallbackMessage);
        }
        String resolved = tryResolveMessage(code, locale).orElse(null);
        if (resolved != null) {
            return resolved;
        }
        return tryResolveMessage("errors." + code, locale).orElse(fallbackMessage);
    }

    private String localizeByMessageKey(String messageKey, String locale, String fallbackMessage) {
        return tryResolveMessage(messageKey, locale).orElse(fallbackMessage);
    }

    private Optional<String> tryResolveMessage(String messageKey, String locale) {
        try {
            return Optional.of(localeBundleApplicationService.resolveMessage(
                    ERROR_BUNDLE_CODE,
                    messageKey,
                    locale,
                    (UUID) null
            ).resourceValue());
        } catch (BizException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private String resolveLocale(HttpServletRequest request) {
        if (request == null) {
            return DEFAULT_LOCALE;
        }
        return normalizeLocale(request.getHeader(ACCEPT_LANGUAGE_HEADER));
    }

    private String normalizeLocale(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_LOCALE;
        }
        String firstLocale = value.split(",", 2)[0].trim().replace('_', '-');
        if (firstLocale.isBlank()) {
            return DEFAULT_LOCALE;
        }
        String[] segments = firstLocale.split("-");
        if (segments.length == 1) {
            return segments[0].toLowerCase(Locale.ROOT);
        }
        StringBuilder builder = new StringBuilder(segments[0].toLowerCase(Locale.ROOT));
        for (int index = 1; index < segments.length; index++) {
            builder.append('-');
            builder.append(index == 1 ? segments[index].toUpperCase(Locale.ROOT) : segments[index]);
        }
        return builder.toString();
    }

    private record MessageCacheKey(String code, String locale, String fallbackMessage) {
    }
}
