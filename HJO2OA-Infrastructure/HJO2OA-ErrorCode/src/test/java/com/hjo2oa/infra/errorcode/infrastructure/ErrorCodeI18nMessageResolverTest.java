package com.hjo2oa.infra.errorcode.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.infra.errorcode.application.ErrorCodeDefinitionApplicationService;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinition;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinitionRepository;
import com.hjo2oa.infra.errorcode.domain.ErrorSeverity;
import com.hjo2oa.infra.i18n.application.I18nCacheInvalidationListener;
import com.hjo2oa.infra.i18n.application.LocaleBundleApplicationService;
import com.hjo2oa.infra.i18n.application.LocaleBundleCommands;
import com.hjo2oa.infra.i18n.infrastructure.InMemoryLocaleBundleRepository;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ErrorCodeI18nMessageResolverTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T03:30:00Z");

    @Test
    void shouldTranslateErrorCodeMessageAndInvalidateWhenI18nBundleChanges() {
        List<I18nCacheInvalidationListener> listeners = new ArrayList<>();
        LocaleBundleApplicationService i18nService = new LocaleBundleApplicationService(
                new InMemoryLocaleBundleRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                () -> listeners
        );
        InMemoryErrorCodeDefinitionRepository errorCodeRepository = new InMemoryErrorCodeDefinitionRepository();
        ErrorCodeDefinitionApplicationService errorCodeService = new ErrorCodeDefinitionApplicationService(
                errorCodeRepository,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        ErrorCodeI18nMessageResolver resolver = new ErrorCodeI18nMessageResolver(errorCodeRepository, i18nService);
        listeners.add(resolver);

        var enBundleId = i18nService.createBundle(new LocaleBundleCommands.CreateBundleCommand(
                "error.messages",
                "infra",
                "en-US",
                null,
                null
        )).id();
        i18nService.addEntry(enBundleId, "infra.error.cache", "English message");
        i18nService.activateBundle(enBundleId);
        var zhBundleId = i18nService.createBundle(new LocaleBundleCommands.CreateBundleCommand(
                "error.messages",
                "infra",
                "zh-CN",
                "en-US",
                null
        )).id();
        i18nService.activateBundle(zhBundleId);
        var definition = errorCodeService.defineCode(
                "INFRA_CACHE_LOCALIZED",
                "infra",
                ErrorSeverity.WARN,
                400,
                "infra.error.cache",
                "cache",
                false
        );

        assertThat(resolver.localize(definition, "zh-CN", "fallback")).isEqualTo("English message");

        i18nService.addEntry(zhBundleId, "infra.error.cache", "Chinese message");

        assertThat(resolver.localize(definition, "zh-CN", "fallback")).isEqualTo("Chinese message");
    }

    @Test
    void shouldUseFallbackMessageWithoutTenantContextWhenResolvingApiError() {
        TenantContextHolder.clear();
        LocaleBundleApplicationService i18nService = new LocaleBundleApplicationService(
                new InMemoryLocaleBundleRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                List::of
        );
        ErrorCodeI18nMessageResolver resolver = new ErrorCodeI18nMessageResolver(
                new ThrowingErrorCodeDefinitionRepository(),
                i18nService
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "zh-CN");

        assertThat(resolver.resolve(SharedErrorDescriptors.TENANT_REQUIRED, "fallback", request))
                .contains("fallback");
    }

    private static final class ThrowingErrorCodeDefinitionRepository implements ErrorCodeDefinitionRepository {

        @Override
        public Optional<ErrorCodeDefinition> findByCode(String code) {
            throw new AssertionError("missing tenant error resolution must not query error code definitions");
        }

        @Override
        public List<ErrorCodeDefinition> findByModule(String moduleCode) {
            throw new AssertionError("missing tenant error resolution must not query error code definitions");
        }

        @Override
        public List<ErrorCodeDefinition> findAll() {
            throw new AssertionError("missing tenant error resolution must not query error code definitions");
        }

        @Override
        public ErrorCodeDefinition save(ErrorCodeDefinition definition) {
            throw new AssertionError("missing tenant error resolution must not query error code definitions");
        }
    }
}
