package com.hjo2oa.infra.errorcode.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.infra.errorcode.domain.ErrorCodeUpdatedEvent;
import com.hjo2oa.infra.errorcode.domain.ErrorSeverity;
import com.hjo2oa.infra.errorcode.infrastructure.InMemoryErrorCodeDefinitionRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorCodeDefinitionApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T03:30:00Z");

    @Test
    void shouldDefineErrorCodeAndPublishUpdatedEvent() {
        List<DomainEvent> events = new ArrayList<>();
        ErrorCodeDefinitionApplicationService applicationService = applicationService(events);

        var created = applicationService.defineCode(
                "INFRA_4001",
                "infra",
                ErrorSeverity.ERROR,
                400,
                "infra.error.invalid",
                "event-bus",
                true
        );

        assertThat(created.code()).isEqualTo("INFRA_4001");
        assertThat(created.moduleCode()).isEqualTo("infra");
        assertThat(created.retryable()).isTrue();
        assertThat(events).singleElement().isInstanceOf(ErrorCodeUpdatedEvent.class);
        ErrorCodeUpdatedEvent event = (ErrorCodeUpdatedEvent) events.get(0);
        assertThat(event.eventType()).isEqualTo("infra.error-code.updated");
        assertThat(event.code()).isEqualTo("INFRA_4001");
        assertThat(event.moduleCode()).isEqualTo("infra");
        assertThat(event.changeType()).isEqualTo("CREATED");
    }

    @Test
    void shouldDeprecateAndRejectFurtherMutations() {
        List<DomainEvent> events = new ArrayList<>();
        ErrorCodeDefinitionApplicationService applicationService = applicationService(events);
        var created = applicationService.defineCode(
                "INFRA_4002",
                "infra",
                ErrorSeverity.WARN,
                429,
                "infra.error.throttled",
                "gateway",
                true
        );
        events.clear();

        var deprecated = applicationService.deprecateCode(created.id());

        assertThat(deprecated.deprecated()).isTrue();
        assertThat(events).singleElement().isInstanceOf(ErrorCodeUpdatedEvent.class);
        assertThat(((ErrorCodeUpdatedEvent) events.get(0)).changeType()).isEqualTo("DEPRECATED");

        assertThatThrownBy(() -> applicationService.updateSeverity(created.id(), ErrorSeverity.ERROR))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已废弃");
    }

    @Test
    void shouldUpdateSeverityHttpStatusAndSupportQueries() {
        ErrorCodeDefinitionApplicationService applicationService = applicationService(new ArrayList<>());
        var first = applicationService.defineCode(
                "INFRA_4003",
                "infra",
                ErrorSeverity.ERROR,
                500,
                "infra.error.failed",
                "scheduler",
                false
        );
        applicationService.defineCode(
                "ORG_4001",
                "org",
                ErrorSeverity.WARN,
                409,
                "org.error.conflict",
                "identity",
                false
        );

        var severityUpdated = applicationService.updateSeverity(first.id(), ErrorSeverity.FATAL);
        var statusUpdated = applicationService.updateHttpStatus(first.id(), 503);

        assertThat(severityUpdated.severity()).isEqualTo(ErrorSeverity.FATAL);
        assertThat(statusUpdated.httpStatus()).isEqualTo(503);
        assertThat(applicationService.queryByCode("INFRA_4003"))
                .isPresent()
                .get()
                .extracting(view -> view.code(), view -> view.httpStatus())
                .containsExactly("INFRA_4003", 503);
        assertThat(applicationService.queryByModule("infra"))
                .singleElement()
                .extracting(view -> view.code(), view -> view.moduleCode())
                .containsExactly("INFRA_4003", "infra");
        assertThat(applicationService.listAll())
                .extracting(view -> view.code())
                .containsExactly("INFRA_4003", "ORG_4001");
    }

    @Test
    void shouldRejectDuplicateCode() {
        ErrorCodeDefinitionApplicationService applicationService = applicationService(new ArrayList<>());
        applicationService.defineCode(
                "INFRA_4090",
                "infra",
                ErrorSeverity.ERROR,
                409,
                "infra.error.conflict",
                null,
                false
        );

        assertThatThrownBy(() -> applicationService.defineCode(
                "INFRA_4090",
                "infra",
                ErrorSeverity.ERROR,
                409,
                "infra.error.conflict",
                null,
                false
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void shouldInvalidateErrorCodeCachesAfterRegistryChanges() {
        CountingInvalidator invalidator = new CountingInvalidator();
        ErrorCodeDefinitionApplicationService applicationService = new ErrorCodeDefinitionApplicationService(
                new InMemoryErrorCodeDefinitionRepository(),
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                List.of(invalidator)
        );

        var created = applicationService.defineCode(
                "INFRA_CACHE_1",
                "infra",
                ErrorSeverity.WARN,
                400,
                "infra.error.cache",
                "cache",
                false
        );
        applicationService.updateDefinition(new ErrorCodeDefinitionCommands.UpdateDefinitionCommand(
                created.id(),
                ErrorSeverity.ERROR,
                409,
                "infra.error.cache.changed",
                "cache",
                true
        ));

        assertThat(invalidator.count).isEqualTo(2);
        assertThat(applicationService.queryByCode("INFRA_CACHE_1"))
                .isPresent()
                .get()
                .extracting(view -> view.severity(), view -> view.httpStatus(), view -> view.messageKey(), view -> view.retryable())
                .containsExactly(ErrorSeverity.ERROR, 409, "infra.error.cache.changed", true);
    }

    private ErrorCodeDefinitionApplicationService applicationService(List<DomainEvent> events) {
        return new ErrorCodeDefinitionApplicationService(
                new InMemoryErrorCodeDefinitionRepository(),
                events::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private static final class CountingInvalidator implements ErrorCodeCacheInvalidator {

        private int count;

        @Override
        public void invalidateErrorCodeCaches() {
            count++;
        }
    }
}
