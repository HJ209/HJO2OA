package com.hjo2oa.infra.errorcode.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ErrorCodeDefinitionTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-24T03:00:00Z");

    @Test
    void shouldCreateAndMutateAggregateRoot() {
        ErrorCodeDefinition definition = ErrorCodeDefinition.create(
                "INFRA_4001",
                "infra",
                "event-bus",
                ErrorSeverity.ERROR,
                400,
                "infra.error.invalid",
                false,
                CREATED_AT
        );

        ErrorCodeDefinition updated = definition.updateSeverity(ErrorSeverity.FATAL, CREATED_AT.plusSeconds(60))
                .updateHttpStatus(503, CREATED_AT.plusSeconds(120))
                .updateMessageKey("infra.error.unavailable", CREATED_AT.plusSeconds(180))
                .markRetryable(CREATED_AT.plusSeconds(240))
                .deprecate(CREATED_AT.plusSeconds(300));

        assertThat(updated.id()).isEqualTo(definition.id());
        assertThat(updated.code()).isEqualTo("INFRA_4001");
        assertThat(updated.category()).isEqualTo("event-bus");
        assertThat(updated.severity()).isEqualTo(ErrorSeverity.FATAL);
        assertThat(updated.httpStatus()).isEqualTo(503);
        assertThat(updated.messageKey()).isEqualTo("infra.error.unavailable");
        assertThat(updated.retryable()).isTrue();
        assertThat(updated.deprecated()).isTrue();
    }

    @Test
    void shouldRejectInvalidHttpStatus() {
        assertThatThrownBy(() -> ErrorCodeDefinition.create(
                "INFRA_4999",
                "infra",
                null,
                ErrorSeverity.ERROR,
                99,
                "infra.error.invalid",
                false,
                CREATED_AT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("httpStatus");
    }
}
